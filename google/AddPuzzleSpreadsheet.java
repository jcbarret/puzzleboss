//For document list API
import com.google.gdata.client.*;
import com.google.gdata.client.docs.*;
import com.google.gdata.data.MediaContent;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.acl.*;
import com.google.gdata.data.docs.*;
import com.google.gdata.data.extensions.*;
import com.google.gdata.util.*;
import java.net.*;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.logging.*;
import sample.util.*;

//For Spreadsheets API
import com.google.gdata.client.spreadsheet.*;
import com.google.gdata.data.*;
import com.google.gdata.data.spreadsheet.*;
import com.google.gdata.util.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//For command line option parser
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.MissingOptionException;

public class AddPuzzleSpreadsheet {
    static DocsService client;
    static String userString = "BigJimmy@stormynight.org";
    static String passString = "";
    static String domainString = "stormynight.org";
    static String versionString = "stormynight-AddPuzzleSpreadsheet-0.2";
    static String commandlineString = "java AddPuzzleSpreadsheet";
    //this was to automagically set the sharing for new docs:    static String parentFolderResourceId = "folder:0B_xwlksahHvUZmMyNTljZmItMGY0Mi00ZjhhLWIxMTItNTM3OGQ2NDNkYTA0";
    static String huntString = "UnknownHunt";
    static String roundString = "UnknownRound";
    static String puzzleString = "UnknownPuzzle";
    static String puzzleTwikiUrlString = "http://toobig2fail.com/";
    static String puzzleUrlString = "http://www.google.com/";

    public static void main(String[] args) {
	// Get command-line arguments
	Options opt = new Options();
	opt.addOption(OptionBuilder.withLongOpt("puzzle").withArgName("PUZZLE_NAME").hasArg().isRequired().withDescription("Puzzle Name (Title for new spreadsheet)").create("p"));
	opt.addOption(OptionBuilder.withLongOpt("domain").withArgName("DOMAIN").hasArg().isRequired().withDescription("Domain in which to create new spreadsheet.").create("d"));
	opt.addOption(OptionBuilder.withLongOpt("round").withArgName("ROUND_NAME").hasArg().isRequired().withDescription("Round Name (Subfolder for new spreadsheet)").create("r"));
	opt.addOption(OptionBuilder.withLongOpt("hunt").withArgName("HUNT_NAME").hasArg().isRequired().withDescription("Hunt Name (Root folder for new spreadsheet)").create("h"));
	opt.addOption(OptionBuilder.withLongOpt("twikiurl").withArgName("TWIKI_URL").hasArg().isRequired().withDescription("The URL to the puzzle TWiki page.").create("t"));
	opt.addOption(OptionBuilder.withLongOpt("puzzleurl").withArgName("PUZZLE_URL").hasArg().isRequired().withDescription("The URL to the puzzle itself").create("u"));
	opt.addOption(OptionBuilder.withLongOpt("adminpass").withArgName("ADMINPASS").hasArg().isRequired().withDescription("Administrator password.").create("a"));
	try {
	    BasicParser parser = new BasicParser();
	    CommandLine cl = parser.parse(opt, args);
	    domainString = cl.getOptionValue('d');
	    passString = cl.getOptionValue('a');
	    puzzleString = cl.getOptionValue('p');
	    roundString = cl.getOptionValue('r');
	    huntString = cl.getOptionValue('h');
	    puzzleTwikiUrlString = cl.getOptionValue('t');
	    puzzleUrlString = cl.getOptionValue('u');
	} catch (MissingOptionException moe) {
	    HelpFormatter formatter = new HelpFormatter();
	    formatter.printHelp( commandlineString, opt );
	    System.exit(-1);
	} catch (ParseException pe) {
	    System.err.println( "Parsing failed.  Reason: " + pe.getMessage() );
	    System.exit(-2);
	}
	
	/*
	Logger httpLogger = Logger.getLogger("com.google.gdata.client.http.HttpGDataRequest");
	httpLogger.setLevel(Level.ALL);
	Logger xmlLogger = Logger.getLogger("com.google.gdata.util.XmlParser");
	xmlLogger.setLevel(Level.ALL);
	ConsoleHandler logHandler = new ConsoleHandler();
	logHandler.setLevel(Level.ALL);
	httpLogger.addHandler(logHandler);
	xmlLogger.addHandler (logHandler);
	*/
	

	// Authenticate
	System.err.println("Attempting to authenticate as user " + userString);
	client = new DocsService(versionString);
	try {
	    client.setUserCredentials(userString, passString);
	} catch (AuthenticationException ae) {
	    ae.printStackTrace();
	    System.exit(-11);
	}
	System.err.println("User authenticated.");


	// Check for root folder and create if necessary
	System.err.println("Checking for root folder "+huntString);
	URL feedUri = null;
	try {
	    feedUri = new URL("https://docs.google.com/feeds/default/private/full/-/folder");
	} catch (MalformedURLException mue) {
	    mue.printStackTrace();
	    System.exit(-21);
	}
	DocumentQuery query = new DocumentQuery(feedUri);
	query.setTitleQuery(huntString);
	query.setTitleExact(true);
	query.setMaxResults(10);
	DocumentListFeed feed = null;
	try {
	    feed = client.getFeed(query, DocumentListFeed.class);
	} catch (MalformedURLException mue) {
	    mue.printStackTrace();
	    System.exit(-22);
	} catch (IOException ioe) {
	    ioe.printStackTrace();
	    System.exit(-23);
	} catch (ServiceException se) {
	    se.printStackTrace();
	    System.exit(-24);
	}
	List<DocumentListEntry> rootFolderEntries = feed.getEntries();
	System.err.println("Found "+rootFolderEntries.size()+" folders called "+huntString);
	DocumentListEntry rootFolderEntry = null;
	if(rootFolderEntries.size() == 1) {
	    rootFolderEntry = rootFolderEntries.get(0);
	} else if(rootFolderEntries.size() > 1) {
	    System.err.println("WARNING: more than one root folder found with the same name, using first one!");
	    rootFolderEntry = rootFolderEntries.get(0);
	} else {
	    // Root folder does not exist yet, create it
	    System.err.println("Creating root folder "+huntString); 
	    try {
		//rootFolderEntry = createFolder(huntString,parentFolderResourceId);
		rootFolderEntry = createFolder(huntString);
	    } catch (IOException ioe) {
		ioe.printStackTrace();
		System.exit(-25);
	    } catch (ServiceException se) {
		se.printStackTrace();
		System.exit(-26);
	    }
	    System.err.println("Created root folder -- Document(" + rootFolderEntry.getResourceId() + "/" + rootFolderEntry.getTitle().getPlainText() + ")");
	    // Wait for root folder to appear in feed
	    System.err.print("Waiting for root folder to appear in feed...");
	    do {
		try {
		    feed = client.getFeed(query, DocumentListFeed.class);
		} catch (MalformedURLException mue) {
		    mue.printStackTrace();
		    System.exit(-27);
		} catch (IOException ioe) {
		    ioe.printStackTrace();
		    System.exit(-28);
		} catch (ServiceException se) {
		    se.printStackTrace();
		    System.exit(-29);
		}
		rootFolderEntries = feed.getEntries();
		if(rootFolderEntries.size() == 0) {
		    System.err.print(".");
		}
	    } while (rootFolderEntries.size() < 1);
	    System.err.println();
	}
	System.err.println("Have root folder -- Document(" + rootFolderEntry.getResourceId() + "/" + rootFolderEntry.getTitle().getPlainText() + ")");

	// update permissions to give whole domain writer privs
	System.err.println("Setting hunt folder ACL to allow domain-wide reads from users at "+domainString);
	setDomainAclRemoveDefault(rootFolderEntry, domainString, "reader");
	
	String huntFeedUriString = ((MediaContent) rootFolderEntry.getContent()).getUri();
	System.err.println("Have hunt root folder URI: " + huntFeedUriString);

	URL huntFeedUri = null;
	try {
	    huntFeedUri = new URL(huntFeedUriString);
	    System.err.println("Have root folder query URL: "+huntFeedUri.toString());
	} catch (MalformedURLException mue) {
	    mue.printStackTrace();
	    System.exit(-31);
	}

	// Check for round subfolder and create if necessary
	System.err.println("Checking for round subfolder "+roundString);
	//DocumentQuery rsfQuery = new DocumentQuery(huntFeedUri);
	// actual query can't be trusted - returns results outside of folder!?
	//	rsfQuery.setTitleQuery(roundString);
	//	rsfQuery.setTitleExact(true);
	//	rsfQuery.setMaxResults(10);
	DocumentListFeed huntfeed = null;
	try {
	    //	    huntfeed = client.getFeed(rsfQuery, DocumentListFeed.class);
	    huntfeed = client.getFeed(huntFeedUri, DocumentListFeed.class);
	} catch (MalformedURLException mue) {
	    mue.printStackTrace();
	    System.exit(-32);
	} catch (IOException ioe) {
	    ioe.printStackTrace();
	    System.exit(-33);
	} catch (ServiceException se) {
	    se.printStackTrace();
	    System.exit(-34);
	}
	//printDocuments(huntfeed);
	List<DocumentListEntry> roundSubfolderEntries = huntfeed.getEntries();
	System.err.println("Found "+roundSubfolderEntries.size()+" subfolders in "+huntString+" -- checking for entry matching round "+roundString);
	DocumentListEntry roundSubfolderEntry = null;
	try {
	    roundSubfolderEntry = getMatchingEntry(huntfeed, roundString, "folder");
	} catch (Exception e) {
	    e.printStackTrace();
	    System.exit(-101);
	}
	
	String roundFolderUriString;
	if(roundSubfolderEntry != null) {
	    roundFolderUriString = ((MediaContent) roundSubfolderEntry.getContent()).getUri();
	    System.err.println("Have round folder URI "+roundFolderUriString);
	} else {
	    // round subfolder does not exist yet, create it
	    System.err.println("Creating round subfolder "+roundString); 
	    try {
		roundSubfolderEntry = createFolder(roundString,rootFolderEntry.getResourceId());
	    } catch (IOException ioe) {
		ioe.printStackTrace();
		System.exit(-35);
	    } catch (ServiceException se) {
		se.printStackTrace();
		System.exit(-36);
	    }
	    System.err.println("Created round subfolder -- Document(" + roundSubfolderEntry.getResourceId() + "/" + roundSubfolderEntry.getTitle().getPlainText() + ")");
	    // Wait for round subfolder to appear in feed
	    System.err.print("Waiting for round subfolder to appear in feed...");
	    do {
		try {
		    huntfeed = client.getFeed(huntFeedUri, DocumentListFeed.class);
		    roundSubfolderEntry = getMatchingEntry(huntfeed, roundString, "folder");
		} catch (MalformedURLException mue) {
		    mue.printStackTrace();
		    System.exit(-37);
		} catch (IOException ioe) {
		    ioe.printStackTrace();
		    System.exit(-38);
		} catch (ServiceException se) {
		    se.printStackTrace();
		    System.exit(-39);
		} catch (Exception e) {
		    e.printStackTrace();
		    System.exit(-101);
		}

		//roundSubfolderEntries = huntfeed.getEntries();
		//if(roundSubfolderEntries.size() == 0) {
		//    System.err.print(".");
		//}
		//} while (roundSubfolderEntries.size() < 1);
		try {
		    Thread.sleep(500);
		} catch (InterruptedException ie) {
		    System.err.print("exception sleeping (probably safe)");
		    //ie.printStackTrace();
		}
		System.err.print(".");
	    } while (roundSubfolderEntry == null);
	    System.err.println();
	}
	System.err.println("Have round subfolder -- Document(" + roundSubfolderEntry.getResourceId() + "/" + roundSubfolderEntry.getTitle().getPlainText() + ")");

	// update permissions to give whole domain writer privs
	System.err.println("Setting round subfolder ACL to allow domain-wide reads from users at "+domainString);
	setDomainAclRemoveDefault(roundSubfolderEntry, domainString, "reader");

	// get Uri for round subfolder feed
	String roundFeedUriString = ((MediaContent) roundSubfolderEntry.getContent()).getUri();
	URL roundFeedUri = null;
	try {
	    roundFeedUri = new URL(roundFeedUriString);
	    System.err.println("Have round folder query URL: "+roundFeedUri.toString());
	} catch (MalformedURLException mue) {
	    mue.printStackTrace();
	    System.exit(-31);
	}


	// check for existing puzzle subfolder and create if necessary
	System.err.println("Checking for puzzle subfolder "+puzzleString);
	DocumentListFeed roundFeed = null;
	try {
	    roundFeed = client.getFeed(roundFeedUri, DocumentListFeed.class);
	} catch (MalformedURLException mue) {
	    mue.printStackTrace();
	    System.exit(-32);
	} catch (IOException ioe) {
	    ioe.printStackTrace();
	    System.exit(-33);
	} catch (ServiceException se) {
	    se.printStackTrace();
	    System.exit(-34);
	}
	//printDocuments(roundFeed);
	List<DocumentListEntry> puzzleSubfolderEntries = roundFeed.getEntries();
	System.err.println("Found "+puzzleSubfolderEntries.size()+" subfolders in "+roundString+" -- checking for entry matching puzzle folder "+puzzleString);
	DocumentListEntry puzzleSubfolderEntry = null;
	try {
	    puzzleSubfolderEntry = getMatchingEntry(roundFeed, puzzleString, "folder");
	} catch (Exception e) {
	    e.printStackTrace();
	    System.exit(-101);
	}
	
	if(puzzleSubfolderEntry != null) {
	    String puzzleSubfolderEntryUriString = ((MediaContent) puzzleSubfolderEntry.getContent()).getUri();
	    System.err.println("Have puzzle folder URI "+puzzleSubfolderEntryUriString);
	} else {
	    // puzzle subfolder does not exist yet, create it
	    System.err.println("Creating puzzle subfolder "+puzzleString); 
	    try {
		puzzleSubfolderEntry = createFolder(puzzleString,roundSubfolderEntry.getResourceId());
	    } catch (IOException ioe) {
		ioe.printStackTrace();
		System.exit(-35);
	    } catch (ServiceException se) {
		se.printStackTrace();
		System.exit(-36);
	    }
	    System.err.println("Created puzzle subfolder -- Document(" + puzzleSubfolderEntry.getResourceId() + "/" + puzzleSubfolderEntry.getTitle().getPlainText() + ")");
	    // Wait for puzzle subfolder to appear in feed
	    System.err.print("Waiting for puzzle subfolder to appear in feed...");
	    do {
		try {
		    roundFeed = client.getFeed(roundFeedUri, DocumentListFeed.class);
		    puzzleSubfolderEntry = getMatchingEntry(roundFeed, puzzleString, "folder");
		} catch (MalformedURLException mue) {
		    mue.printStackTrace();
		    System.exit(-37);
		} catch (IOException ioe) {
		    ioe.printStackTrace();
		    System.exit(-38);
		} catch (ServiceException se) {
		    se.printStackTrace();
		    System.exit(-39);
		} catch (Exception e) {
		    e.printStackTrace();
		    System.exit(-101);
		}

		try {
		    Thread.sleep(250);
		} catch (InterruptedException ie) {
		    System.err.print("exception sleeping (probably safe)");
		    //ie.printStackTrace();
		}
		System.err.print(".");
	    } while (puzzleSubfolderEntry == null);
	    System.err.println();
	}
	System.err.println("Have puzzle subfolder -- Document(" + puzzleSubfolderEntry.getResourceId() + "/" + puzzleSubfolderEntry.getTitle().getPlainText() + ")");

	// update permissions to give whole domain writer privs
	System.err.println("Setting puzzle subfolder ACL to allow domain-wide writes from users at "+domainString);
	setDomainAclRemoveDefault(puzzleSubfolderEntry, domainString, "writer");

	// String puzzleSubfolderUriString = puzzleSubfolderEntry.getDocumentLink().getHref();
	String puzzleSubfolderUriString = "https://docs.google.com/a/" + domainString + "/#folders/" + puzzleSubfolderEntry.getDocId();
	System.out.println("subfolder="+puzzleSubfolderUriString);


	// get Uri for puzzle subfolder feed
	String puzzlefeedUriString = ((MediaContent) puzzleSubfolderEntry.getContent()).getUri();
	URL puzzlefeedUri = null;
	try {
	    puzzlefeedUri = new URL(puzzlefeedUriString);
	    System.err.println("Have puzzle folder query URL: "+puzzlefeedUri.toString());
	} catch (MalformedURLException mue) {
	    mue.printStackTrace();
	    System.exit(-31);
	}
	
	
	// check for existing spreadsheet for this puzzle
	System.err.println("Checking for puzzle "+puzzleString);
	DocumentListFeed puzzlefeed = null;
	try {
	    puzzlefeed = client.getFeed(puzzlefeedUri, DocumentListFeed.class);
	} catch (MalformedURLException mue) {
	    mue.printStackTrace();
	    System.exit(-32);
	} catch (IOException ioe) {
	    ioe.printStackTrace();
	    System.exit(-33);
	} catch (ServiceException se) {
	    se.printStackTrace();
	    System.exit(-34);
	}
	//printDocuments(puzzlefeed);
	List<DocumentListEntry> puzzleEntries = puzzlefeed.getEntries();
	System.err.println("Found "+puzzleEntries.size()+" documents in "+puzzleString+" folder");
	DocumentListEntry puzzleSpreadsheetEntry = null;
	try{ 
	    puzzleSpreadsheetEntry = getMatchingEntry(puzzlefeed, puzzleString,"spreadsheet");
	} catch (Exception e) {
	    e.printStackTrace();
	    System.exit(-101);
	}
	
	if(puzzleSpreadsheetEntry != null) {
	    System.err.println("Warning: puzzle already exists");
	    //will output URL anyway
	} else {
	    // puzz does not exist, create it
	    System.err.println("Creating new spreadsheet called "+puzzleString);
	    puzzleSpreadsheetEntry = new com.google.gdata.data.docs.SpreadsheetEntry();
	    puzzleSpreadsheetEntry.setTitle(new PlainTextConstruct(puzzleString));
	    puzzleSpreadsheetEntry.setWritersCanInvite(false);
	    //puzzleSpreadsheetEntry.setHidden(true);
	    try {
		//puzzleSpreadsheetEntry = client.insert(new URL("https://docs.google.com/feeds/default/private/full/"+puzzleSpreadsheetEntry.getResourceId()+"/contents"), puzzleSpreadsheetEntry);
		puzzleSpreadsheetEntry = client.insert(puzzlefeedUri, puzzleSpreadsheetEntry);
	    } catch (MalformedURLException mue) {
		mue.printStackTrace();
		System.exit(-41);
	    } catch (IOException ioe) {
		ioe.printStackTrace();
		System.exit(-42);
	    } catch (ServiceException se) {
		se.printStackTrace();
		System.exit(-43);
	    }
	    System.err.println("Puzzle spreadsheet has been created");
	}
	//String puzzleSpreadsheetUriString = ((MediaContent) puzzleSpreadsheetEntry.getContent()).getUri();
	String puzzleSpreadsheetUriString = puzzleSpreadsheetEntry.getDocumentLink().getHref();
	System.err.println("Spreadsheet has been created and is now online @ " + puzzleSpreadsheetUriString);
	System.out.println("spreadsheet="+puzzleSpreadsheetUriString);

	// update permissions to give whole domain writer privs
	System.err.println("Setting spreadsheet ACL to allow domain-wide writes from users at "+domainString);
	setDomainAclRemoveDefault(puzzleSpreadsheetEntry, domainString, "writer");
	
	// Now set initial document contents
	
	// Authenticate to Spreadsheets service
	System.err.println("Now creating contents. Attempting to authenticate to SpreadsheetService as user " + userString);
	SpreadsheetService ssService = new SpreadsheetService(versionString);
	try {
	    ssService.setUserCredentials(userString, passString);
	} catch (AuthenticationException ae) {
	    ae.printStackTrace();
	    System.exit(-51);
	}
	System.err.println("User authenticated.");

	URL ssURL = null;
	try {
	    ssURL = new URL("http://spreadsheets.google.com/feeds/spreadsheets/private/full/"+puzzleSpreadsheetEntry.getDocId());
	} catch (MalformedURLException mue) {
	    mue.printStackTrace();
	    System.exit(-61);
	}
	
	com.google.gdata.data.spreadsheet.SpreadsheetEntry ssEntry = null;
	try {
	    System.err.println("SpreadsheetService: getting "+ssURL);
	    ssEntry = ssService.getEntry(ssURL, com.google.gdata.data.spreadsheet.SpreadsheetEntry.class, new String());
	} catch (IOException ioe) {
	    ioe.printStackTrace();
	    System.exit(-62);
	} catch (ServiceException se) {
	    se.printStackTrace();
	    System.exit(-63);
	}

	List<WorksheetEntry> worksheets = null; 
	try {
	    System.err.println("SpreadsheetEntry: getting worksheets");
	    worksheets = ssEntry.getWorksheets();
	} catch (IOException ioe) {
	    ioe.printStackTrace();
	    System.exit(-64);
	} catch (ServiceException se) {
	    se.printStackTrace();
	    System.exit(-65);
	}
	System.err.println("Have "+worksheets.size()+" worksheets");
	WorksheetEntry worksheet = worksheets.get(0);
	String currTitle = worksheet.getTitle().getPlainText();
	System.err.println("Have worksheet titled: "+currTitle);
	if (currTitle.equals("Sheet 1")) { // only edit if it hasn't been edited yet
	    worksheet.setTitle(new PlainTextConstruct("Work on "+puzzleString));
	    worksheet.setRowCount(100);
	    worksheet.setColCount(26);
	    try {
		System.err.println("WorksheetEntry: update()"); 
		worksheet.update();
	    } catch (IOException ioe) {
		ioe.printStackTrace();
		System.exit(-66);
	    } catch (ServiceException se) {
		se.printStackTrace();
		System.exit(-67);
	    }
	
	    //Add second worksheet for work
	    WorksheetEntry newWorksheet = new WorksheetEntry();
	    newWorksheet.setTitle(new PlainTextConstruct(puzzleString+" metadata"));
	    newWorksheet.setRowCount(6);
	    newWorksheet.setColCount(4);
	    
	    try {
		URL worksheetFeedUrl = ssEntry.getWorksheetFeedUrl();
		newWorksheet = ssService.insert(worksheetFeedUrl,newWorksheet);
	    } catch (IOException ioe) {
		ioe.printStackTrace();
		System.exit(-74);
	    } catch (ServiceException se) {
		se.printStackTrace();
		System.exit(-75);
	    }
	    
	    URL cellFeedUrl = newWorksheet.getCellFeedUrl();
	    try {
		ssService.insert(cellFeedUrl, new CellEntry(1,1,"Round:"));
		ssService.insert(cellFeedUrl, new CellEntry(1,2,roundString));	
		ssService.insert(cellFeedUrl, new CellEntry(2,1,"Puzzle:"));
		ssService.insert(cellFeedUrl, new CellEntry(2,2,puzzleString));	
		ssService.insert(cellFeedUrl, new CellEntry(3,1,"Puzzle page on TWiki:"));
		ssService.insert(cellFeedUrl, new CellEntry(3,2,puzzleTwikiUrlString));
		ssService.insert(cellFeedUrl, new CellEntry(4,1,"Actual Puzzle URL:"));
		ssService.insert(cellFeedUrl, new CellEntry(4,2,puzzleUrlString));
		ssService.insert(cellFeedUrl, new CellEntry(6,1,"N.B. - This sheet just has links to the puzzle -- please use the other worksheet for work (see below), and feel free to create as many more sheets as you need! (using \"Add Sheet\" button, which is the + symbol below)"));
		//ssService.insert(cellFeedUrl, new CellEntry(6,4,"If possible, please login to google with your own account before editing this spreadsheet so that we can tell who did what in case we need to ask questions later on!"));
	    } catch (IOException ioe) {
		ioe.printStackTrace();
		System.exit(-72);
	    } catch (ServiceException se) {
		se.printStackTrace();
		System.exit(-73);
	    }
	    System.err.println("Done creating spreadsheet contents!");
	} else {
	    System.err.println("Spreadsheet contents already edited, skipping.");
	}
    }
    
    
    static public AclEntry addAclRole(AclRole role, AclScope scope, DocumentListEntry entry)
	throws IOException, MalformedURLException, ServiceException  {
	AclEntry aclEntry = new AclEntry();
	aclEntry.setRole(role);
	aclEntry.setScope(scope);
	
	return client.insert(new URL(entry.getAclFeedLink().getHref()), aclEntry);
    }

    static public DocumentListEntry createFolder(String title) throws IOException, ServiceException {
	DocumentListEntry newEntry = new FolderEntry();
	newEntry.setTitle(new PlainTextConstruct(title));
	URL feedUrl = new URL("https://docs.google.com/feeds/default/private/full/");
	return client.insert(feedUrl, newEntry);
    }

    static public DocumentListEntry createFolder(String title, String resource) throws IOException, ServiceException {
	DocumentListEntry newEntry = new FolderEntry();
	newEntry.setTitle(new PlainTextConstruct(title));
	URL feedUrl = new URL("https://docs.google.com/feeds/default/private/full/"+resource+"/contents");
	return client.insert(feedUrl, newEntry);
    }

    static public DocumentListEntry createNewDocument(String title, String type, URL uri)
	throws IOException, ServiceException {
	DocumentListEntry newEntry = null;
	if (type.equals("document")) {
	    newEntry = new DocumentEntry();
	} else if (type.equals("presentation")) {
	    newEntry = new PresentationEntry();
	} else if (type.equals("spreadsheet")) {
	    newEntry = new com.google.gdata.data.docs.SpreadsheetEntry();
	} else if (type.equals("folder")) {
	    newEntry = new FolderEntry();
	}
	newEntry.setTitle(new PlainTextConstruct(title));

	return client.insert(uri, newEntry);
    }

    static public DocumentListEntry createNewDocument(String title, String type)
	throws MalformedURLException, IOException, ServiceException {
	return createNewDocument(title, type, new URL("https://docs.google.com/feeds/default/private/full/"));
    }

    static public DocumentListEntry getMatchingEntry(DocumentListFeed feed, String matchTitleString, String matchTypeString) throws Exception {
	DocumentListEntry foundEntry = null;
	int foundCount = 0;
	for (DocumentListEntry entry : feed.getEntries()) {
	    String entryTitleString = entry.getTitle().getPlainText();
	    String entryTypeString = entry.getType();
	    //String resourceId = entry.getResourceId();
	    //System.err.println(" -- Document(" + resourceId + "/" + entry.getTitle().getPlainText() + ")");
	    //System.err.println("Have "+entryTypeString+" entry ["+entryTitleString+"]");
	    if(entryTypeString.equals(matchTypeString) && entryTitleString.equals(matchTitleString)) {
		String resourceId = entry.getResourceId(); 
		System.err.println("Identified matching "+entryTypeString+" entry "+entryTitleString+": "+resourceId);
		foundEntry = entry;
		foundCount++;
	    }
	}
	if(foundCount > 1) {
	    throw new Exception("Found multiple result entries ["+foundCount+"]");
	} else {
	    return foundEntry;
	}
    }

    static public void setDomainAclRemoveDefault(DocumentListEntry entry, String domainString, String roleString) {
	String aclFeedUriString = entry.getAclFeedLink().getHref();
	AclFeed aclFeed = null;
	try {
	    aclFeed = client.getFeed(new URL(aclFeedUriString), AclFeed.class);
	} catch (MalformedURLException mue) {
	    mue.printStackTrace();
	    System.exit(-132);
	} catch (IOException ioe) {
	    ioe.printStackTrace();
	    System.exit(-133);
	} catch (ServiceException se) {
	    se.printStackTrace();
	    System.exit(-134);
	}
	for (AclEntry aclEntry : aclFeed.getEntries()) {
	    System.err.println("Have ACL entry: "+aclEntry.getScope().getValue() + " (" + aclEntry.getScope().getType() + ") : " + aclEntry.getRole().getValue());
	    // delete ACL for anyone-with-a-link: 'null (DEFAULT) : writer'
	    if(aclEntry.getScope().getType().toString().equals("DEFAULT")) {
		try {
		    // the update fails if it has withKey set (access with the link), delete it instead
		    aclEntry.delete();
		} catch (ServiceException se) {
		    se.printStackTrace();
		    System.exit(-201);
		} catch (UnsupportedOperationException uoe) {
		    uoe.printStackTrace();
		    System.exit(-202);
		} catch (IOException ioe) {
		    ioe.printStackTrace();
		    System.exit(-203);
		}
		System.err.println("DEFAULT ACL entry deleted");
	    }
	    
	    // delete DOMAIN entry for this domain unless the role is already writer
	    if(aclEntry.getScope().getType().toString().equals("DOMAIN") && aclEntry.getScope().getValue().toString().equals(domainString)) {
		if(aclEntry.getRole().toString().equals(roleString)) {
		    System.err.println("ACL entry for DOMAIN "+domainString+" is already "+roleString);
		} else {
		    // delete ACL so it can be added below
		    // NOTE: we need to delete and add rather than update because the update fails if it has withKey set (access with the link)
		    try {
			aclEntry.delete();
		    } catch (ServiceException se) {
			se.printStackTrace();
			System.exit(-201);
		    } catch (UnsupportedOperationException uoe) {
			uoe.printStackTrace();
			System.exit(-202);
		    } catch (IOException ioe) {
			ioe.printStackTrace();
			System.exit(-203);
		    }
		    System.err.println("ACL entry deleted");
		}
	    }
	}
	System.err.println("Adding ACL entry for spreadsheet to entire domain "+domainString);
	AclRole role = new AclRole(roleString);
	AclScope scope = new AclScope(AclScope.Type.DOMAIN, domainString);
	try {
	    AclEntry domainAclEntry = addAclRole(role, scope, entry);
	} catch (VersionConflictException vce) {
	    System.err.println("VersionConflictException: "+vce);
	    
	} catch (InvalidEntryException iee) {
	    System.err.println("InvalidEntryException");
	    iee.printStackTrace();
	    System.exit(-110);
	} catch (MalformedURLException mue) {
	    mue.printStackTrace();
	    System.exit(-111);
	} catch (IOException ioe) {
	    ioe.printStackTrace();
	    System.exit(-112);
	} catch (ServiceException se) {
	    se.printStackTrace();
	    System.exit(-113);
	}
    }

    // helper function to dump documents in a list feed
    static public void printDocuments(DocumentListFeed feed) {
	for (DocumentListEntry entry : feed.getEntries()) {
	    String resourceId = entry.getResourceId();
	    System.err.println(" -- Document(" + resourceId + "/" + entry.getTitle().getPlainText() + ")");
	}
    }
}

