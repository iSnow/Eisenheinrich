package net.krautchan.android.activity;

import net.krautchan.data.KCThread;

public interface ProvidesThreads {
	KCThread getThread(long dbId);
}
