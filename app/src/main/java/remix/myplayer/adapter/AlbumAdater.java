package remix.myplayer.adapter;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import butterknife.BindView;
import remix.myplayer.R;
import remix.myplayer.adapter.holder.BaseViewHolder;
import remix.myplayer.asynctask.AsynLoadImage;
import remix.myplayer.asynctask.AsynLoadSongNum;
import remix.myplayer.fragment.AlbumFragment;
import remix.myplayer.listener.AlbArtFolderPlaylistListener;
import remix.myplayer.model.MultiPosition;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.MultiChoice;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DensityUtil;

/**
 * Created by Remix on 2015/12/20.
 */

/**
 * 专辑界面的适配器
 */
public class AlbumAdater extends BaseAdapter<AlbumAdater.AlbumHolder>  {
    private MultiChoice mMultiChoice;
    public AlbumAdater(Cursor cursor, Context context,MultiChoice multiChoice) {
        super(context,cursor);
        this.mMultiChoice = multiChoice;
    }

    @Override
    public int getItemViewType(int position) {
        return AlbumFragment.getModel();
    }

    @Override
    public AlbumHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return viewType == Constants.LIST_MODEL ?
                new AlbumListHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_album_recycle_list,parent,false)) :
                new AlbumGridHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_album_recycle_grid,parent,false));
    }


    @Override
    public void onBindViewHolder(final AlbumHolder holder, final int position) {
        if(mCursor.moveToPosition(position)) {
            try {
                //获得并设置专辑与艺术家
                String artist = CommonUtil.processInfo(mCursor.getString(AlbumFragment.mArtistIndex),CommonUtil.ARTISTTYPE);
                String album = CommonUtil.processInfo(mCursor.getString(AlbumFragment.mAlbumIndex),CommonUtil.ALBUMTYPE);

                holder.mText1.setText(album);
                holder.mText2.setText(artist);
                //设置封面
                int albumid = mCursor.getInt(AlbumFragment.mAlbumIdIndex);
                holder.mImage.setImageURI(Uri.EMPTY);
                new AsynLoadImage(holder.mImage).execute(albumid,Constants.URL_ALBUM,true);
                if(holder instanceof AlbumListHolder){
                    new AsynLoadSongNum(holder.mText2,Constants.ALBUM).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,albumid);
                }
//                holder.mImage.setImageURI(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), mCursor.getInt(AlbumFragment.mAlbumIdIndex)));
            } catch (Exception e){
                e.printStackTrace();
            }

            if(mOnItemClickLitener != null) {
                holder.mContainer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mOnItemClickLitener.onItemClick(holder.mContainer,holder.getAdapterPosition());
                    }
                });
                //多选菜单
                holder.mContainer.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        mOnItemClickLitener.onItemLongClick(holder.mContainer,holder.getAdapterPosition());
                        return true;
                    }
                });

            }

            //popupmenu
            if(holder.mButton != null) {
                int tintColor = ThemeStore.THEME_MODE == ThemeStore.DAY ? ColorUtil.getColor(R.color.gray_6c6a6c) : Color.WHITE;
                Theme.TintDrawable(holder.mButton,R.drawable.list_icn_more,tintColor);
                holder.mButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(mMultiChoice.isShow())
                            return;
                        Context wrapper = new ContextThemeWrapper(mContext,Theme.getPopupMenuStyle());
                        final PopupMenu popupMenu = new PopupMenu(wrapper,holder.mButton,Gravity.END);
                        popupMenu.getMenuInflater().inflate(R.menu.album_menu, popupMenu.getMenu());
                        mCursor.moveToPosition(holder.getAdapterPosition());
                        popupMenu.setOnMenuItemClickListener(new AlbArtFolderPlaylistListener(mContext,
                                mCursor.getInt(AlbumFragment.mAlbumIdIndex),
                                Constants.ALBUM,
                                mCursor.getString(AlbumFragment.mAlbumIndex)));
                        popupMenu.show();
                    }
                });
            }

            //是否处于选中状态
            if(MultiChoice.TAG.equals(AlbumFragment.TAG) &&
                    mMultiChoice.mSelectedPosition.contains(new MultiPosition(position))){
                mMultiChoice.AddView(holder.mContainer);
            } else {
                holder.mContainer.setSelected(false);
            }

            //设置padding
            if(AlbumFragment.ListModel == 2 && holder.mRoot != null){
                if(position % 2 == 0){
                    holder.mRoot.setPadding(DensityUtil.dip2px(mContext,6),0,DensityUtil.dip2px(mContext,3),DensityUtil.dip2px(mContext,8));
                } else {
                    holder.mRoot.setPadding(DensityUtil.dip2px(mContext,3),0,DensityUtil.dip2px(mContext,6),DensityUtil.dip2px(mContext,8));
                }
            }
        }
    }

    public static class AlbumHolder extends BaseViewHolder {
        @BindView(R.id.item_text1)
        public TextView mText1;
        @BindView(R.id.item_text2)
        public TextView mText2;
        @BindView(R.id.item_button)
        public ImageButton mButton;
        @BindView(R.id.item_simpleiview)
        public SimpleDraweeView mImage;
        @BindView(R.id.item_container)
        public RelativeLayout mContainer;
        @BindView(R.id.item_root)
        @Nullable
        public View mRoot;
        public AlbumHolder(View v) {
            super(v);
        }
    }

    public static class AlbumGridHolder extends AlbumHolder {
        public AlbumGridHolder(View v) {
            super(v);
        }
    }

    public static class AlbumListHolder extends AlbumHolder {
        public AlbumListHolder(View v) {
            super(v);
        }
    }

}
