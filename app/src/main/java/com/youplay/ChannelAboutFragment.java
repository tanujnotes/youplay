package com.youplay;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by tan on 28/01/17.
 **/

public class ChannelAboutFragment extends Fragment {

    private final String TAG = "ChannelAboutFragment";

    public static ChannelAboutFragment getInstance(String channelTitle, String channelId, String channelThumbnail, String channelDescription) {
        ChannelAboutFragment channelAboutFragment = new ChannelAboutFragment();

        Bundle args = new Bundle();
        args.putString("channel_title", channelTitle);
        args.putString("channel_id", channelId);
        args.putString("channel_thumbnail", channelThumbnail);
        args.putString("channel_description", channelDescription);
        channelAboutFragment.setArguments(args);
        return channelAboutFragment;
    }

    private View root;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (root != null) return root;
        root = inflater.inflate(R.layout.fragment_channel_about, container, false);

        Bundle bundle = getArguments();
        String channelTitle = bundle.getString("channel_title");
        final String channelId = bundle.getString("channel_id");
        String channelThumbnail = bundle.getString("channel_thumbnail");
        String channelDescription = bundle.getString("channel_description");
        if (channelDescription != null && channelDescription.isEmpty())
            channelDescription = "No description found for this channel";

        ((TextView) root.findViewById(R.id.channel_title)).setText(channelTitle);
        ((TextView) root.findViewById(R.id.channel_description)).setText(channelDescription);

        return root;
    }
}