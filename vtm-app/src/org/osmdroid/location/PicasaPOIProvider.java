package org.osmdroid.location;

import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;
import org.osmdroid.utils.HttpConnection;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * POI Provider using Picasa service.
 *
 * @author M.Kergall
 * @see "https://developers.google.com/picasa-web/docs/2.0/reference"
 */
public class PicasaPOIProvider implements POIProvider {

    private static final Logger log = Logger.getLogger(PicasaPOIProvider.class.getName());

    String mAccessToken;

    /**
     * @param accessToken the account to give to the service. Null for public access.
     * @see "https://developers.google.com/picasa-web/docs/2.0/developers_guide_protocol#CreatingAccount"
     */
    public PicasaPOIProvider(String accessToken) {
        mAccessToken = accessToken;
    }

    @SuppressWarnings("deprecation")
    private String getUrlInside(BoundingBox boundingBox, int maxResults, String query) {
        StringBuffer url = new StringBuffer("http://picasaweb.google.com/data/feed/api/all?");
        url.append("bbox=" + boundingBox.getMinLongitude());
        url.append("," + boundingBox.getMinLatitude());
        url.append("," + boundingBox.getMaxLongitude());
        url.append("," + boundingBox.getMaxLatitude());
        url.append("&max-results=" + maxResults);
        url.append("&thumbsize=64c"); //thumbnail size: 64, cropped.
        url.append("&fields=openSearch:totalResults,entry(summary,media:group/media:thumbnail,media:group/media:title,gphoto:*,georss:where,link)");
        if (query != null)
            url.append("&q=" + URLEncoder.encode(query));
        if (mAccessToken != null) {
            //TODO: warning: not tested...
            url.append("&access_token=" + mAccessToken);
        }
        return url.toString();
    }

    public ArrayList<POI> getThem(String fullUrl) {
        log.fine("PicasaPOIProvider:get:" + fullUrl);
        HttpConnection connection = new HttpConnection();
        connection.doGet(fullUrl);
        InputStream stream = connection.getStream();
        if (stream == null) {
            return null;
        }
        PicasaXMLHandler handler = new PicasaXMLHandler();
        try {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            parser.getXMLReader().setFeature("http://xml.org/sax/features/namespaces", false);
            parser.getXMLReader()
                    .setFeature("http://xml.org/sax/features/namespace-prefixes", true);
            parser.parse(stream, handler);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        connection.close();
        if (handler.mPOIs != null)
            log.fine("done:" + handler.mPOIs.size() + " got on a total of:"
                    + handler.mTotalResults);
        return handler.mPOIs;
    }

    /**
     * @param boundingBox ...
     * @param maxResults  ...
     * @param query       - optional - full-text query string. Searches the title,
     *                    caption and tags for the specified string value.
     * @return list of POI, Picasa photos inside the bounding box. Null if
     * technical issue.
     */
    @Override
    public List<POI> getPOIInside(BoundingBox boundingBox, String query, int maxResults) {
        String url = getUrlInside(boundingBox, maxResults, query);
        return getThem(url);
    }
}

class PicasaXMLHandler extends DefaultHandler {

    private String mString;
    double mLat, mLng;
    POI mPOI;
    ArrayList<POI> mPOIs;
    int mTotalResults;

    public PicasaXMLHandler() {
        mPOIs = new ArrayList<POI>();
    }

    @Override
    public void startElement(String uri, String localName, String qName,
                             Attributes attributes) {
        if (qName.equals("entry")) {
            mPOI = new POI(POI.POI_SERVICE_PICASA);
        } else if (qName.equals("media:thumbnail")) {
            mPOI.thumbnailPath = attributes.getValue("url");
        } else if (qName.equals("link")) {
            String rel = attributes.getValue("rel");
            if ("http://schemas.google.com/photos/2007#canonical".equals(rel)) {
                mPOI.url = attributes.getValue("href");
            }
        }
        mString = new String();
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        String chars = new String(ch, start, length);
        mString = mString.concat(chars);
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if (qName.equals("gml:pos")) {
            String[] coords = mString.split(" ");
            mLat = Double.parseDouble(coords[0]);
            mLng = Double.parseDouble(coords[1]);
        } else if (qName.equals("gphoto:id")) {
            mPOI.id = mString;
        } else if (qName.equals("media:title")) {
            mPOI.type = mString;
        } else if (qName.equals("summary")) {
            mPOI.description = mString;
        } else if (qName.equals("gphoto:albumtitle")) {
            mPOI.category = mString;
        } else if (qName.equals("entry")) {
            mPOI.location = new GeoPoint(mLat, mLng);
            mPOIs.add(mPOI);
            mPOI = null;
        } else if (qName.equals("openSearch:totalResults")) {
            mTotalResults = Integer.parseInt(mString);
        }
    }

}
