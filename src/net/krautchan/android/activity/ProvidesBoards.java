package net.krautchan.android.activity;

import net.krautchan.data.KCBoard;

public interface ProvidesBoards {
	public KCBoard getBoard (long dbId);
}
