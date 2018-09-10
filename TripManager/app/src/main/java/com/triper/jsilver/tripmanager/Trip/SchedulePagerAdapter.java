package com.triper.jsilver.tripmanager.Trip;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.ViewGroup;

/**
 * Created by JSilver on 2017-10-08.
 */

public class SchedulePagerAdapter extends FragmentPagerAdapter {
    private Fragment[] fragments;

    public SchedulePagerAdapter(FragmentManager fm, int days) {
        super(fm);
        fragments = new Fragment[days];
        for(int i = 0; i < days; i++)
            fragments[i] = TripScheduleFragment.newFragment(i + 1);
    }

    @Override
    public Fragment getItem(int index) {
        return fragments[index];
    }

    @Override
    public int getCount() {
        return fragments.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return null;
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }
}
