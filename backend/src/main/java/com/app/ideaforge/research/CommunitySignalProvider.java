package com.app.ideaforge.research;

import java.util.List;

/** Extra demand/competition signal from community discussions. */
public interface CommunitySignalProvider {

	List<CommunitySignal> search(String query, int maxResults);
}
