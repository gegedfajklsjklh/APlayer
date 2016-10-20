package remix.myplayer.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import remix.myplayer.R;
import remix.myplayer.adapter.PlayListAdapter;
import remix.myplayer.db.PlayLists;
import remix.myplayer.interfaces.OnItemClickListener;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.ListItemDecoration;
import remix.myplayer.ui.MultiChoice;
import remix.myplayer.ui.activity.ChildHolderActivity;
import remix.myplayer.ui.activity.MultiChoiceActivity;
import remix.myplayer.util.Constants;
import remix.myplayer.util.Global;
import remix.myplayer.util.PlayListUtil;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.ToastUtil;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/10/8 09:46
 */
public class PlayListFragment extends BaseFragment implements LoaderManager.LoaderCallbacks<Cursor>{
    public static final String TAG = PlayListFragment.class.getSimpleName();
    private static int LOADER_ID = 0;
    public static PlayListFragment mInstance = null;
    public static int mPlayListIDIndex;
    public static int mPlayListNameIndex;
    public static int mPlayListSongCountIndex;
    private Cursor mCursor;
    @BindView(R.id.playlist_recycleview)
    RecyclerView mRecycleView;

    //列表显示与网格显示切换
    @BindView(R.id.list_model)
    ImageView mListModelBtn;
    @BindView(R.id.grid_model)
    ImageView mGridModelBtn;
    //当前列表模式 1:列表 2:网格
    public static int ListModel = 2;

    private PlayListAdapter mAdapter;
    private MultiChoice mMultiChoice;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mPageName = TAG;
        getLoaderManager().initLoader(LOADER_ID++, null, this);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_playlist,null);
        mUnBinder = ButterKnife.bind(this,rootView);

        ListModel = SPUtil.getValue(getActivity(),"Setting","PlayListModel",2);
        mRecycleView.setLayoutManager(ListModel == 1 ? new LinearLayoutManager(getActivity()) : new GridLayoutManager(getActivity(), 2));
        if(ListModel == Constants.LIST_MODEL){
            mRecycleView.addItemDecoration(new ListItemDecoration(getActivity(),ListItemDecoration.VERTICAL_LIST));
        }
        if(getActivity() instanceof MultiChoiceActivity){
            mMultiChoice = ((MultiChoiceActivity) getActivity()).getMultiChoice();
        }
        mAdapter = new PlayListAdapter(getActivity(),mMultiChoice);
        mAdapter.setOnItemClickLitener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                String name = getPlayListName(position);
                //多选状态下我的收藏列表不能被选中
                if(mMultiChoice.isShow() && name.equals(getString(R.string.my_favorite))){
                    return;
                }
                if(!TextUtils.isEmpty(name) && !mMultiChoice.itemAddorRemoveWithClick(view,position,getPlayListId(position),TAG)){
                    if(getPlayListSongCount(position) == 0) {
                        Toast.makeText(getActivity(), getString(R.string.list_isempty), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Intent intent = new Intent(getActivity(), ChildHolderActivity.class);
                    intent.putExtra("Id", getPlayListId(position));
                    intent.putExtra("Title", name);
                    intent.putExtra("Type", Constants.PLAYLIST);
                    intent.putExtra("PlayListID", getPlayListId(position));
                    startActivity(intent);
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {
                String name = getPlayListName(position);
                if(!TextUtils.isEmpty(name) && !name.equals(getString(R.string.my_favorite)))
                    mMultiChoice.itemAddorRemoveWithLongClick(view,position,getPlayListId(position),TAG);
            }
        });
        mRecycleView.setAdapter(mAdapter);

        mListModelBtn.setImageDrawable(Theme.getPressAndSelectedStateListDrawalbe(getActivity(),R.drawable.btn_list2));
        mListModelBtn.setSelected(ListModel == Constants.LIST_MODEL);

        mGridModelBtn.setImageDrawable(Theme.getPressAndSelectedStateListDrawalbe(getActivity(),R.drawable.btn_list1));
        mGridModelBtn.setSelected(ListModel == Constants.GRID_MODEL);

        return rootView;
    }

    public static synchronized int getModel(){
        return ListModel;
    }

    private int getPlayListId(int position){
        int playListId = -1;
        if(mCursor != null && !mCursor.isClosed() && mCursor.moveToPosition(position)){
            playListId = mCursor.getInt(PlayListFragment.mPlayListIDIndex);
        }
        return playListId;
    }

    private String getPlayListName(int position){
        String playlistName = "";
        if(mCursor != null && !mCursor.isClosed() && mCursor.moveToPosition(position)){
            playlistName = mCursor.getString(PlayListFragment.mPlayListNameIndex);
        }
        return playlistName;
    }

    private int getPlayListSongCount(int position){
        int count = 0;
        if(mCursor != null && !mCursor.isClosed() && mCursor.moveToPosition(position)){
            count = mCursor.getInt(PlayListFragment.mPlayListSongCountIndex);
        }
        return count;
    }

    //打开添加播放列表的Dialog
    @OnClick({R.id.list_model,R.id.grid_model})
    public void onClick(View v){
        switch (v.getId()){
            case R.id.list_model:
            case R.id.grid_model:
                int newModel = v.getId() == R.id.list_model ? Constants.LIST_MODEL : Constants.GRID_MODEL;
                if(newModel == ListModel)
                    return;
                mListModelBtn.setSelected(v.getId() == R.id.list_model);
                mGridModelBtn.setSelected(v.getId() == R.id.grid_model);
                ListModel = newModel;
                mRecycleView.setLayoutManager(ListModel == Constants.LIST_MODEL ? new LinearLayoutManager(getActivity()) : new GridLayoutManager(getActivity(), 2));
                if(ListModel == Constants.LIST_MODEL){
                    mRecycleView.addItemDecoration(new ListItemDecoration(getActivity(),ListItemDecoration.VERTICAL_LIST));
                }
                SPUtil.putValue(getActivity(),"Setting","PlayListModel",ListModel);
                break;

        }
    }

    @Override
    public RecyclerView.Adapter getAdapter() {
        return mAdapter;
    }

    public void UpdateAdapter() {
        if(mAdapter != null){
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), PlayLists.CONTENT_URI,
                null,
                PlayLists.PlayListColumns.NAME + "!= ?",new String[]{Constants.PLAY_QUEUE},null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(data == null)
            return;
        //查询完毕后保存结果，并设置查询索引
        try {
            mCursor = data;
            mPlayListIDIndex = data.getColumnIndex(PlayLists.PlayListColumns._ID);
            mPlayListNameIndex = data.getColumnIndex(PlayLists.PlayListColumns.NAME);
            mPlayListSongCountIndex = data.getColumnIndex(PlayLists.PlayListColumns.COUNT);
            mAdapter.setCursor(data);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mAdapter != null)
            mAdapter.setCursor(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mCursor != null) {
            mCursor.close();
        }
        if(mAdapter != null){
            mAdapter.setCursor(null);
        }
    }
}