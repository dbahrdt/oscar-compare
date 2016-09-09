package de.fmi.ocse;

import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.File;
import java.io.IOException;

import org.apache.commons.configuration.ConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.funroll_loops.oscar.OsmCompleter;

public class Worker {

	private final int debug_cost = Helpers.DebugCost.none;
	private final int debug_type = Helpers.DebugType.constant;

	private class SearchResultDifference {
		public ArrayList<Integer> onlyInFirst = new ArrayList<Integer>();
		public ArrayList<Integer> onlyInSecond = new ArrayList<Integer>();
		public ArrayList<Integer> inBoth = new ArrayList<Integer>();
		SearchResultDifference(SearchResultInterface first, SearchResultInterface second) {
			int fIt = 0;
			int sIt = 0;
			int fEnd = first.size();
			int sEnd = second.size();
			int last = 0;
			for(; fIt != fEnd && sIt != sEnd;) {
				int fId = first.at(fIt);
				int sId = second.at(sIt);
				if (fId < last ||  sId < last) {
					throw new java.lang.RuntimeException("Input sequences are not sorted!");
				}
				if (fId == sId) {
					inBoth.add(fId);
					last = fId;
					++fIt;
					++sIt;
				}
				else if (fId < sId) {
					onlyInFirst.add(fId);
					last = fId;
					++fIt;
				}
				else {
					onlyInSecond.add(sId);
					last = sId;
					++sIt;
				}
			}
			for(; fIt != fEnd; ++fIt) {
				onlyInFirst.add(first.at(fIt));
			}
			for(; sIt != sEnd; ++sIt) {
				onlyInSecond.add(second.at(sIt));
			}
		}
	}

	private Config m_cfg;
	private OsmCompleter m_cmp;
	private SearchInterface[] m_searches;
	private SearchQueryInterface[] m_searchQueries;
	private ArrayList<SearchQueryNode> m_q;
	
	Worker() {
		m_q = new ArrayList<SearchQueryNode>();
	}
	
	public void init(Config cfg) throws IOException {
		m_cfg = cfg;
		m_cmp = new OsmCompleter();
		System.out.println("Setting path of oscar files to " + cfg.oscarPath.toString());
		m_cmp.setFilePrefix(cfg.oscarPath.toString());
		System.out.println("Energizing!");
		m_cmp.energize();
		
		if (m_cfg.buildSearches > 0) {
			System.out.println("Initalizing indexing config");
			m_cfg.idxConfig.init(m_cmp.store());
			System.out.println("Initalized indexing config");
		}
		
		m_searches = new SearchInterface[Config.SearchType.Last+1];
		m_searchQueries = new SearchQueryInterface[Config.SearchType.Last+1];

		//lucene
		if (((m_cfg.buildSearches | m_cfg.activeSearches) & Config.SearchType.Lucene) != 0) {
			m_searches[Config.SearchType.Lucene] = new LuceneSearch(m_cmp.store(), m_cfg.idxConfig, m_cfg.lucenePath);
		}
		if ((m_cfg.activeSearches & Config.SearchType.Lucene) != 0) {
			m_searchQueries[Config.SearchType.Lucene] = new LuceneSearchQuery();
		}

		//mg4j
		if (((m_cfg.activeSearches | m_cfg.buildSearches) & Config.SearchType.Mg4j) != 0) {
			m_searches[Config.SearchType.Mg4j] = new MG4JSearch(m_cmp.store(), m_cfg.idxConfig, m_cfg.mg4jPath);

		}
		if ((m_cfg.activeSearches & Config.SearchType.Mg4j) != 0) {
			m_searchQueries[Config.SearchType.Mg4j] = new Mg4jSearchQuery();
		}

		//oscar
		if (((m_cfg.activeSearches | m_cfg.buildSearches) & Config.SearchType.Oscar) != 0) {
			m_searches[Config.SearchType.Oscar] = new OscarSearch(m_cmp);
		}
		if ((m_cfg.activeSearches & Config.SearchType.Oscar) != 0) {
			m_searchQueries[Config.SearchType.Oscar] = new OscarSearchQuery();
		}
	}

	public void loadQueries() {
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			return;
		} 
		Document doc;
		try {
			doc = db.parse(new File(m_cfg.queriesPath.toString()));
		} catch (SAXException | IOException e) {
			e.printStackTrace();
			return;
		}
		assert( doc.getFirstChild().getNodeName() == "ocse" );
		NodeList queries = doc.getFirstChild().getChildNodes();
		for(int i = 0; i < queries.getLength(); ++i) {
			Node query = queries.item(i);
			if (query.getNodeType() != Node.ELEMENT_NODE || query.getNodeName() != "query") {
				continue;
			}
			NodeList queryChildren = query.getChildNodes();
			for(int j = 0; j < queryChildren.getLength(); ++j) {
				Node queryChild = queryChildren.item(j);
				if (queryChild.getNodeType() != Node.ELEMENT_NODE) {
					continue;
				}
				SearchQueryNode qn = SearchQueryNode.fromXml(queryChild);
				if ((debug_type & Helpers.DebugType.print_debug_msg) != 0) {
					System.out.println("Found query: " + qn.toString());
				}
				m_q.add(qn);
			}
		}
	}
	
	public int queryCount() {
		return m_q.size();
	}
	
	public void index() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, IOException, InstantiationException, URISyntaxException, ConfigurationException, ClassNotFoundException {
		for(int type : Config.SearchType.iterate) {
			if ((m_cfg.buildSearches & type) != 0) {
				m_searches[type].index();
			}
		}
	}
	
	public QueryStats query() throws IOException {
		TimeMeasurer tm = new TimeMeasurer();
		TimeMeasurer ttm = new TimeMeasurer();
		QueryStats qs = new QueryStats();
		for(int type : Config.SearchType.iterate) {
			if ((m_cfg.activeSearches & type) == 0) {
				continue;
			}
			m_searches[type].load();

			System.out.print("Querying " + m_searches[type].name() + "...");
			ttm.start();
			for(SearchQueryNode qn : m_q) {
				SearchResultInterface result;
				try {
					m_searchQueries[type].init(qn);
					tm.start();
					result = m_searches[type].search(m_searchQueries[type]);
					tm.stop();
				} catch (UnsupportedQueryType unsupportedQueryType) {
					System.out.println(m_searches[type].toString() + ": " + unsupportedQueryType.toString());
					qs.add(m_searches[type].type(), new SingleResultStat(new EmptySearchResult(), 0) );
					continue;
				}
				qs.add(m_searches[type].type(), new SingleResultStat(result, tm.elapsedNsecs()) );
				if ((debug_type & Helpers.DebugType.print_debug_msg) != 0) {
					System.out.println(qn.toString() + "=" + m_searchQueries[type].toString() + "->" + result.size());
				}
			}
			ttm.stop();
			System.out.print("took " + ttm.elapsedMsecs() + " ms\n");
		}
		return qs;
	}

	public void queryDifferenceToOscar() throws IOException {
		SearchResultInterface[] results = new SearchResultInterface[m_searches.length];
		SearchResultDifference[] diffs = new SearchResultDifference[m_searches.length];

		for (int type : Config.SearchType.iterate) {
			if ((m_cfg.activeSearches & type) == 0) {
				continue;
			}
			m_searches[type].load();
		}
		int queryCount = 0;
		for(SearchQueryNode qn : m_q) {
			boolean isDifferent = false;
			for (int type : Config.SearchType.iterate) {
				if ((m_cfg.activeSearches & type) == 0) {
					continue;
				}
				try {
					m_searchQueries[type].init(qn);
					results[type] = m_searches[type].search(m_searchQueries[type]);
				}
				catch (UnsupportedQueryType e) {
					System.out.println(Config.SearchType.names[type] + " does not support the given query: " + e.toString());
					results[type] = null;
					continue;
				}
			}
			for (int type : Config.SearchType.iterate) {
				if ((m_cfg.activeSearches & type) == 0 || results[type] == null) {
					continue;
				}
				diffs[type] = new SearchResultDifference(results[type], results[Config.SearchType.Oscar]);
				isDifferent |= diffs[type].onlyInFirst.size() > 0 || diffs[type].onlyInSecond.size() > 0;
			}
			if (!isDifferent) {
				continue;
			}
			System.out.println("BEGIN Difference to oscar for query " + qn.toString());
			for (int type : Config.SearchType.iterate) {
				if ((m_cfg.activeSearches & type) == 0 || results[type] == null) {
					continue;
				}
				if ((debug_type & Helpers.DebugType.print_debug_msg) != 0) {
					if (debug_cost >= Helpers.DebugCost.expensive) {
						for (int oscarId : diffs[type].onlyInFirst) {
							m_searches[type].debugDoc(oscarId);
						}
						for (int oscarId : diffs[type].onlyInSecond) {
							m_searches[type].debugDoc(oscarId);
						}
					}
					else if (debug_cost == Helpers.DebugCost.normal ) {
						System.out.print("Only in " + Config.SearchType.names[type] + ": ");
						System.out.print(diffs[type].onlyInFirst);
						System.out.print("\n");
						System.out.print("Only in OSCAR: ");
						System.out.print(diffs[type].onlyInSecond);
						System.out.print("\n");
					}
				}

				System.out.print("\toscar - " + Config.SearchType.names[type] + ":" + diffs[type].onlyInSecond.size() + "\n");
				System.out.print("\t" + Config.SearchType.names[type] + " - oscar:" + diffs[type].onlyInFirst.size() + "\n");
				System.out.print("\toscar \u2229 " + Config.SearchType.names[type] + ":" + diffs[type].inBoth.size() + "\n");
			}

			System.out.println("END Difference to oscar for query " + qn.toString());
		}
	}
}
