package jp.co.osstech.jeidreader;

import android.nfc.Tag;

public interface TagDiscoveredListener {
    void onTagDiscovered(Tag tag);
}
