package com.google.gdata.client.youtube.crawler;

import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javax.xml.transform.TransformerConfigurationException;

import org.jgraph.graph.DefaultEdge;
import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;
import org.jgrapht.ext.EdgeNameProvider;
import org.jgrapht.ext.GmlExporter;
import org.jgrapht.ext.GraphMLExporter;
import org.jgrapht.ext.IntegerEdgeNameProvider;
import org.jgrapht.ext.IntegerNameProvider;
import org.jgrapht.ext.StringEdgeNameProvider;
import org.jgrapht.ext.StringNameProvider;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.xml.sax.SAXException;

import com.google.gdata.client.youtube.YouTubeQuery;
import com.google.gdata.client.youtube.YouTubeService;
import com.google.gdata.data.BaseEntry;
import com.google.gdata.data.youtube.VideoEntry;
import com.google.gdata.data.youtube.VideoFeed;
import com.google.gdata.util.ServiceException;

public class youtubeCrawler {
	
	private static final int MAX_NODES = 500;
	private static final String keyword = "Linux";
	
	private static final String FILENAME = "/home/abhi/NetTheory/hw/YouTubeGraph.gml";
	private static final String YOUTUBE_GDATA_SERVER = "http://gdata.youtube.com";
	
	/**
	 * The URL of the "Videos" feed
	 */
	private static final String VIDEOS_FEED = YOUTUBE_GDATA_SERVER
	      + "/feeds/api/videos";

	private static  DirectedGraph<String, DefaultEdge> ytGraph; 
	
	
	/**
	 * @param videoFeed
	 * @param ve
	 */
	private static void addDetailToGraph(VideoFeed videoFeed, VideoEntry ve) {
			
		for(BaseEntry<VideoEntry> be : videoFeed.getEntries()) {
			VideoEntry ve1 = (VideoEntry)be;
			if(!ytGraph.containsVertex(ve1.getMediaGroup().getVideoId())) {
				System.out.println("New Vertex : " + ve1.getMediaGroup().getVideoId());
				ytGraph.addVertex(remSpecChar(ve1.getTitle().getPlainText()));
				ytGraph.addEdge(remSpecChar(ve.getTitle().getPlainText()), remSpecChar(ve1.getTitle().getPlainText()));
			}
			else if(!ytGraph.containsEdge(ve.getMediaGroup().getVideoId(), ve1.getMediaGroup().getVideoId())) {
				System.out.println("New Edge from " + ve.getMediaGroup().getVideoId() + " to" +  ve1.getMediaGroup().getVideoId());
				ytGraph.addEdge(remSpecChar(ve.getTitle().getPlainText()), remSpecChar(ve1.getTitle().getPlainText()));
			}
		}
	}
	
	/**
	 * @param service
	 * @param ve
	 * @return
	 */
	private static VideoFeed searchForRelatedVideos(YouTubeService service, VideoEntry ve) {

		String feedurl = null;
		VideoFeed vf = null;
		
		if(ve.getRelatedVideosLink() != null) {
			feedurl = ve.getRelatedVideosLink().getHref();
		}

		try {
			
		 vf = service.getFeed(new URL(feedurl),VideoFeed.class);
		 
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch(ServiceException e) {
			e.printStackTrace();
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		return vf;

	}
	
	/**
	 * @param service
	 * @param keyword
	 * @return
	 */
	private static VideoFeed searchFeed(YouTubeService service, String keyword) {

		YouTubeQuery query;
		VideoFeed videoFeed = null;
		try {
			query = new YouTubeQuery(new URL(VIDEOS_FEED));

			query.setOrderBy(YouTubeQuery.OrderBy.RELEVANCE);
			query.setSafeSearch(YouTubeQuery.SafeSearch.NONE);
			query.setFullTextQuery(keyword);
			System.out.println("Query Object Created. Running search for  : " + query.getFullTextQuery());

			videoFeed = service.query(query, VideoFeed.class);

		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch(ServiceException e) {
			e.printStackTrace();
		} catch(IOException e) {
			e.printStackTrace();
		}

		return videoFeed;

	}

	/**
	 * @param service
	 * @param ve
	 */
	private static void BFS(YouTubeService service,VideoEntry ve){
		VideoFeed videoFeed = searchForRelatedVideos(service,ve);
		addDetailToGraph(videoFeed,ve);
		LinkedList<VideoEntry> queue = new LinkedList<VideoEntry>(); 
		
		while(ytGraph.vertexSet().size() < MAX_NODES) {
			queue.addFirst(ve);
			while(!queue.isEmpty()) {
				VideoEntry ve1 = (VideoEntry) queue.removeLast();
				System.out.println("VideoID being processed : " + ve1.getMediaGroup().getVideoId());
				VideoFeed vf1 = searchForRelatedVideos(service,ve1);
				for(VideoEntry ve2 : vf1.getEntries()){
					queue.addFirst(ve2);
				}
				addDetailToGraph(vf1, ve1);
				
				if(ytGraph.vertexSet().size() > MAX_NODES) 
					break;
			}
		
		}	
	}
	
	/**
	 * @param g
	 */
	private static void printVertices(Graph<String,DefaultEdge> g) {
		System.out.println("Printing Vertices in Graph --- ");
		for(String v : g.vertexSet()) {
			System.out.println(v);
		}
	}
	
	
	private static void exportGraphToGML(FileWriter fw,Graph g) {

		IntegerNameProvider inp = new IntegerNameProvider<String>();
		IntegerEdgeNameProvider<DefaultEdge> inep = new IntegerEdgeNameProvider<DefaultEdge>();
		StringNameProvider<String> snp = new StringNameProvider<String>();
		StringEdgeNameProvider<DefaultEdge> senp = new StringEdgeNameProvider<DefaultEdge>();
		
		GmlExporter<String, DefaultEdge> gmx = new GmlExporter<String, DefaultEdge>(inp,snp,inep,senp);
		gmx.setPrintLabels(org.jgrapht.ext.GmlExporter.PRINT_EDGE_VERTEX_LABELS);
		gmx.export(fw, (DefaultDirectedWeightedGraph<String,DefaultEdge>)g);
	}
	
	private static void exportGraphToGraphML(FileWriter fw, Graph g)  {
		IntegerNameProvider inp = new IntegerNameProvider<String>();
		IntegerEdgeNameProvider<DefaultEdge> inep = new IntegerEdgeNameProvider<DefaultEdge>();
		StringNameProvider<String> snp = new StringNameProvider<String>();
		StringEdgeNameProvider<DefaultEdge> senp = new StringEdgeNameProvider<DefaultEdge>();
		
		GraphMLExporter<String,DefaultEdge> gmx = new GraphMLExporter<String,DefaultEdge>(inp,snp,inep,senp);
		try {
			gmx.export(fw,g);
		} catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static String remSpecChar(String str) {
		   StringBuilder sb = new StringBuilder();
		   for(int i = 0; i < str.length(); i++) {
		      if ((str.charAt(i) >= '0' && str.charAt(i) <= '9') || (str.charAt(i) >= 'A' && str.charAt(i) <= 'Z') || (str.charAt(i) >= 'a' && str.charAt(i) <= 'z') || str.charAt(i) == '.' || str.charAt(i)  == '_' || str.charAt(i) == '-' || str.charAt(i) == ' ') {	
		         sb.append(str.charAt(i));
		      }
		   }
		   return sb.toString();
	}
	/**
	 * @param args
	 */
	public static void main(String [] args) {
		
		YouTubeService service = new YouTubeService("YouTube Crawer");
		ytGraph = new DefaultDirectedWeightedGraph<String,DefaultEdge>(DefaultEdge.class);
				
		VideoFeed videoFeed = searchFeed(service,keyword);
		List<VideoEntry> lve = videoFeed.getEntries();
		String videoID = lve.get(0).getMediaGroup().getVideoId();
		System.out.println(" Video ID " + videoID);
		ytGraph.addVertex(remSpecChar(lve.get(0).getTitle().getPlainText()));
		
		BFS(service,lve.get(0));
		System.out.println("Number of vertices : " + ytGraph.vertexSet().size());
		printVertices(ytGraph);
		
		FileWriter fileGraph = null;
		try {
			fileGraph = new FileWriter(FILENAME);
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		exportGraphToGML(fileGraph,ytGraph);
		//exportGraphToGraphML(fileGraph, ytGraph);
		
	}
}	