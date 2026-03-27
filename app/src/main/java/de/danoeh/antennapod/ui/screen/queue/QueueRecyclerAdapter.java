package de.danoeh.antennapod.ui.screen.queue;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;

import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.ui.episodeslist.EpisodeItemListAdapter;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.swipeactions.SwipeActions;
import de.danoeh.antennapod.ui.episodeslist.EpisodeItemViewHolder;

/**
 * List adapter for the queue.
 */
public class QueueRecyclerAdapter extends EpisodeItemListAdapter {
    private static final String TAG = "QueueRecyclerAdapter";

    private final SwipeActions swipeActions;
    private boolean dragDropEnabled;


    public QueueRecyclerAdapter(FragmentActivity mainActivity, SwipeActions swipeActions) {
        super(mainActivity);
        this.swipeActions = swipeActions;
        dragDropEnabled = ! (UserPreferences.isQueueKeepSorted() || UserPreferences.isQueueLocked());
    }

    public void updateDragDropEnabled() {
        dragDropEnabled = ! (UserPreferences.isQueueKeepSorted() || UserPreferences.isQueueLocked());
        notifyDataSetChanged();
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    protected void afterBindViewHolder(EpisodeItemViewHolder holder, int pos) {
        if (!dragDropEnabled) {
            holder.queueReorderControls.setVisibility(View.GONE);
            holder.dragHandle.setOnTouchListener(null);
            holder.coverHolder.setOnTouchListener(null);
            holder.queueReorderUp.setOnClickListener(null);
            holder.queueReorderDown.setOnClickListener(null);
        } else if (inActionMode()) {
            holder.queueReorderControls.setVisibility(View.GONE);
            holder.dragHandle.setOnTouchListener(null);
            holder.coverHolder.setOnTouchListener(null);
            holder.queueReorderUp.setOnClickListener(null);
            holder.queueReorderDown.setOnClickListener(null);
        } else {
            holder.queueReorderControls.setVisibility(View.VISIBLE);
            holder.dragHandle.setOnTouchListener((v1, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    Log.d(TAG, "startDrag()");
                    swipeActions.startDrag(holder);
                }
                return false;
            });
            holder.coverHolder.setOnTouchListener((v1, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    boolean isLtr = holder.itemView.getLayoutDirection() == View.LAYOUT_DIRECTION_LTR;
                    float factor = isLtr ? 1 : -1;
                    if (factor * event.getX() < factor * 0.5 * v1.getWidth()) {
                        Log.d(TAG, "startDrag()");
                        swipeActions.startDrag(holder);
                    } else {
                        Log.d(TAG, "Ignoring drag in right half of the image");
                    }
                }
                return false;
            });
            int adapterPos = holder.getBindingAdapterPosition();
            boolean atTop = adapterPos == RecyclerView.NO_POSITION || adapterPos <= 0;
            boolean atBottom = adapterPos == RecyclerView.NO_POSITION
                    || adapterPos >= getItemCount() - 1;
            holder.queueReorderUp.setEnabled(!atTop);
            holder.queueReorderUp.setAlpha(atTop ? 0.38f : 1f);
            holder.queueReorderDown.setEnabled(!atBottom);
            holder.queueReorderDown.setAlpha(atBottom ? 0.38f : 1f);
            holder.queueReorderUp.setOnClickListener(v -> {
                int p = holder.getBindingAdapterPosition();
                if (p == RecyclerView.NO_POSITION || p <= 0) {
                    return;
                }
                FeedItem item = getItem(p);
                moveQueueItemToTopInEpisodeList(p);
                DBWriter.moveQueueItemsToTop(Collections.singletonList(item));
            });
            holder.queueReorderDown.setOnClickListener(v -> {
                int p = holder.getBindingAdapterPosition();
                if (p == RecyclerView.NO_POSITION || p >= getItemCount() - 1) {
                    return;
                }
                FeedItem item = getItem(p);
                moveQueueItemToBottomInEpisodeList(p);
                DBWriter.moveQueueItemsToBottom(Collections.singletonList(item));
            });
        }

        holder.isInQueue.setVisibility(View.GONE);
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.queue_context, menu);
        super.onCreateContextMenu(menu, v, menuInfo);

        if (!inActionMode()) {
            menu.findItem(R.id.multi_select).setVisible(true);
            final boolean keepSorted = UserPreferences.isQueueKeepSorted();
            if (getItem(0).getId() == getLongPressedItem().getId() || keepSorted) {
                menu.findItem(R.id.move_to_top_item).setVisible(false);
            }
            if (getItem(getItemCount() - 1).getId() == getLongPressedItem().getId() || keepSorted) {
                menu.findItem(R.id.move_to_bottom_item).setVisible(false);
            }
        } else {
            menu.findItem(R.id.move_to_top_item).setVisible(false);
            menu.findItem(R.id.move_to_bottom_item).setVisible(false);
        }
    }
}
