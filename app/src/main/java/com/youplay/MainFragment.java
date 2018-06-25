package com.youplay;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by tan on 22/04/17.
 **/

public class MainFragment extends Fragment implements TabLayout.OnTabSelectedListener {
    private final String TAG = "MainFragment";

    private ViewPager viewPager;
    private View root;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (root != null) return root;
        root = inflater.inflate(R.layout.fragment_main, container, false);
        ((MyApp) getActivity().getApplication()).getNetComponent().inject(this);

        TabLayout tabLayout = root.findViewById(R.id.main_tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText("TRENDING"));
        tabLayout.addTab(tabLayout.newTab().setText("MUSIC"));
        tabLayout.addTab(tabLayout.newTab().setText("COMEDY"));
        tabLayout.addTab(tabLayout.newTab().setText("SPORTS"));
        tabLayout.addTab(tabLayout.newTab().setText("TECH"));
//        tabLayout.setTabGravity(TabLayout.MODE_SCROLLABLE);
        tabLayout.setSelectedTabIndicatorColor(Color.WHITE);
        tabLayout.addOnTabSelectedListener(this);

        viewPager = root.findViewById(R.id.main_viewpager);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        viewPager.setOffscreenPageLimit(2);
        viewPager.setAdapter(new MainFragmentPagerAdapter((getChildFragmentManager())));

        return root;
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

    private class MainFragmentPagerAdapter extends FragmentPagerAdapter {
        int PAGES = 5;

        MainFragmentPagerAdapter(android.app.FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return FeaturedMainFragment.getInstance(24); //Entertainment
                case 1:
                    return FeaturedMainFragment.getInstance(10); //Music
                case 2:
                    return FeaturedMainFragment.getInstance(23); //Comedy
                case 3:
                    return FeaturedMainFragment.getInstance(17); //Sports
                case 4:
                    return FeaturedMainFragment.getInstance(28); //Tech
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
        ((ToolbarAndFabInterface) getActivity()).setupToolbar(AppConstants.YOUPLAY, true);
    }
}