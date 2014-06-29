package com.ryanharter.atv.launcher.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.ryanharter.atv.launcher.LauncherAppState;
import com.ryanharter.atv.launcher.R;
import com.ryanharter.atv.launcher.loader.AppLoader;
import com.ryanharter.atv.launcher.model.AppInfo;
import com.ryanharter.atv.launcher.widget.TextClock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import it.sephiroth.android.library.widget.HListView;

/**
 * Created by rharter on 1/11/14.
 */
public class AppListFragment extends Fragment implements LoaderCallbacks<List<AppInfo>> {

    private TextClock mClock;
    private ImageButton mAllAppsButton;
    private HListView mAppsList;
    private LinearLayout mFavorites;

    private AppAdapter mAdapter;

    static List<String> favorites = Arrays.asList("Play Movies & TV", "Netflix", "Plex", "Phone", "Clock", "Camera");

    interface Callbacks {
        void onExpandButtonClick();
    }

    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override public void onExpandButtonClick() {}
    };

    private Callbacks mCallbacks = sDummyCallbacks;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_app_list, container, false);

        mClock = (TextClock) v.findViewById(R.id.clock);

        mAllAppsButton = (ImageButton) v.findViewById(R.id.expand_button);
        mAllAppsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mCallbacks.onExpandButtonClick();
            }
        });

        mAppsList = (HListView) v.findViewById(R.id.app_list);
//        mAppsList.setOnItemClickListener(new OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//                final AppInfo info = mAdapter.getItem(i);
//                startActivity(info.intent);
//            }
//        });

        mAppsList.setOnItemClickListener(new it.sephiroth.android.library.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(it.sephiroth.android.library.widget.AdapterView<?> parent, View view, int position, long id) {
                final AppInfo info = mAdapter.getItem(position);
                startActivity(info.intent);
            }
        });

        mFavorites = (LinearLayout) v.findViewById(R.id.favorite_bar);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState == null) {
            getLoaderManager().restartLoader(0, null, this);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
//        if (!(activity instanceof Callbacks)) {
//            throw new IllegalArgumentException("Parent activity must implement Callbacks.");
//        }
//        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        mCallbacks = sDummyCallbacks;
        super.onDetach();
    }

    @Override
    public Loader<List<AppInfo>> onCreateLoader(int i, Bundle bundle) {
        return new AppLoader(getActivity(), LauncherAppState.getInstance().getIconCache());
    }

    @Override
    public void onLoadFinished(Loader<List<AppInfo>> listLoader, List<AppInfo> appInfos) {

        // Take out the favorites
        appInfos = extractFavorites(appInfos);

        mAdapter = new AppAdapter(getActivity(), appInfos);
        mAppsList.setAdapter(mAdapter);
    }

    @Override
    public void onLoaderReset(Loader<List<AppInfo>> listLoader) {
        mAppsList.setAdapter(null);
    }

    private List<AppInfo> extractFavorites(List<AppInfo> infos) {
        List<AppInfo> favs = new ArrayList<>(favorites.size());
        for (String name : favorites) {
            for (AppInfo info : infos) {
                if (name.equals(info.title)) {
                    favs.add(info);
                    infos.remove(info);
                    break;
                }
            }
        }

        mFavorites.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        for (AppInfo info : favs) {
            View v = inflater.inflate(R.layout.row_app, mFavorites, false);
            LayoutParams params = new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            v.setLayoutParams(params);

            ImageView image = (ImageView) v.findViewById(R.id.image);
            TextView title = (TextView) v.findViewById(R.id.title);
            image.setImageDrawable(new BitmapDrawable(getResources(), info.iconBitmap));
            title.setText(info.title);

            final Intent appIntent = info.intent;
            v.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(appIntent);
                }
            });

            mFavorites.addView(v);
        }

        return infos;
    }

    class AppAdapter extends ArrayAdapter<AppInfo>  {


        int mResource;
        int mTextResId;

        private LayoutInflater mLayoutInflater;
        private Resources mResources;

        public AppAdapter(Context context, List<AppInfo> objects) {
            super(context, 0, objects);
            mLayoutInflater = LayoutInflater.from(context);
            mResources = context.getResources();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            ViewHolder holder;
            if (v == null) {
                v = mLayoutInflater.inflate(R.layout.row_app, parent, false);
                holder = new ViewHolder();

                holder.image = (ImageView) v.findViewById(R.id.image);
                holder.title = (TextView) v.findViewById(R.id.title);

                v.setTag(holder);
            } else {
                holder = (ViewHolder) v.getTag();
            }

            final AppInfo info = getItem(position);
            holder.image.setImageDrawable(getCustomIcon(info));

            holder.title.setText(info.title);

            return v;
        }

        private BitmapDrawable getCustomIcon(AppInfo info) {
            if (info.packageName.equals("com.estrongs.android.pop")) {
                return ((BitmapDrawable)mResources.getDrawable(R.drawable.explorer));
            }
            if (info.packageName.equals("org.videolan.vlc.betav7neon")) {
                return ((BitmapDrawable)mResources.getDrawable(R.drawable.vlc));
            }
            if (info.packageName.equals("com.jetappfactory.jetaudio")) {
                return ((BitmapDrawable)mResources.getDrawable(R.drawable.audio));
            }
            if (info.packageName.equals("com.android.chrome")) {
                return ((BitmapDrawable)mResources.getDrawable(R.drawable.chrome));
            }
            if (info.packageName.equals("com.skype.raider")) {
                return ((BitmapDrawable)mResources.getDrawable(R.drawable.skype));
            }
            if (info.packageName.equals("com.android.play")) {
                return ((BitmapDrawable)mResources.getDrawable(R.drawable.play));
            }
            if (info.packageName.equals("com.android.settings")) {
                return ((BitmapDrawable)mResources.getDrawable(R.drawable.settings));
            }
            if (info.packageName.equals("com.google.android.youtube")) {
                return ((BitmapDrawable)mResources.getDrawable(R.drawable.youtube));
            }
            return new BitmapDrawable(mResources, info.iconBitmap);
        }

        class ViewHolder {
            ImageView image;
            TextView title;
        }
    }


    public class AppAdapterOrig extends ArrayAdapter<AppInfo> {

        private LayoutInflater mLayoutInflater;
        private Resources mResources;

        public AppAdapterOrig(Context context, List<AppInfo> objects) {
            super(context, 0, objects);
            mLayoutInflater = LayoutInflater.from(context);
            mResources = context.getResources();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            ViewHolder holder;
            if (v == null) {
                v = mLayoutInflater.inflate(R.layout.row_app, parent, false);
                holder = new ViewHolder();

                holder.image = (ImageView) v.findViewById(R.id.image);
                holder.title = (TextView) v.findViewById(R.id.title);

                v.setTag(holder);
            } else {
                holder = (ViewHolder) v.getTag();
            }

            final AppInfo info = getItem(position);

            holder.image.setImageDrawable(new BitmapDrawable(mResources, info.iconBitmap));
            holder.title.setText(info.title);

            return v;
        }



        class ViewHolder {
            ImageView image;
            TextView title;
        }
    }
}
