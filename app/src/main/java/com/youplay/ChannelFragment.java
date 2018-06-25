package com.youplay;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashMap;

import javax.inject.Inject;

import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by tan on 20/01/17.
 **/

public class ChannelFragment extends Fragment implements TabLayout.OnTabSelectedListener {
    private final String TAG = "ChannelFragment";

    @Inject
    NetworkService networkService;

    private String channelTitle, channelId, channelThumbnail, channelDescription;
    private ViewPager viewPager;
    private View root, loadingView;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (root != null) return root;
        root = inflater.inflate(R.layout.fragment_channel, container, false);
        ((MyApp) getActivity().getApplication()).getNetComponent().inject(this);

        Bundle bundle = getArguments();
        channelTitle = bundle.getString("channel_title");
        channelId = bundle.getString("channel_id");
        channelThumbnail = bundle.getString("channel_thumbnail");

        loadingView = root.findViewById(R.id.channel_loading_view);
        TabLayout tabLayout = root.findViewById(R.id.channel_tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText("HOME"));
        tabLayout.addTab(tabLayout.newTab().setText("PLAYLIST"));
        tabLayout.addTab(tabLayout.newTab().setText("ABOUT"));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.setSelectedTabIndicatorColor(Color.WHITE);
        tabLayout.addOnTabSelectedListener(this);

        viewPager = root.findViewById(R.id.channel_viewpager);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        viewPager.setOffscreenPageLimit(2);
        getChannelDetails();

        return root;
    }

    private void getChannelDetails() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("part", "snippet");
        map.put("maxResults", 1);
        map.put("id", channelId);
        map.put("key", BuildConfig.youtube_key);
        Observable<ItemModel> observable = networkService.getChannelDetails(map);
        observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ItemModel>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "Error: " + e.getMessage());
                        if (Utils.isInternetAvailable(getActivity(), true))
                            Utils.showToast(getActivity().getApplicationContext(), "Oops! Please try again.");
                    }

                    @Override
                    public void onNext(ItemModel model) {
                        loadingView.setVisibility(View.GONE);
                        channelTitle = model.getItems().get(0).getSnippet().getTitle();
                        channelThumbnail = model.getItems().get(0).getSnippet().getThumbnails().getMedium().getUrl();
                        channelDescription = model.getItems().get(0).getSnippet().getDescription();
                        viewPager.setAdapter(new ChannelPagerAdapter((getChildFragmentManager())));
                    }
                });
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        viewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {

    }

    private class ChannelPagerAdapter extends FragmentPagerAdapter {
        int PAGES = 3;

        ChannelPagerAdapter(android.app.FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return ChannelHomeFragment.getInstance(channelTitle, channelId, channelThumbnail);
                case 1:
                    return ChannelPlaylistFragment.getInstance(channelTitle, channelId, channelThumbnail);
                case 2:
                    return ChannelAboutFragment.getInstance(channelTitle, channelId, channelThumbnail, channelDescription);
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return PAGES;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ((ToolbarAndFabInterface) getActivity()).setupToolbar(channelTitle, false);
//        ((ToolbarAndFabInterface) getActivity()).setupToolbarImage(playlistThumbnail);
    }
}