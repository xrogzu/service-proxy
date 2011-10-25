package com.predic8.membrane.core.interceptor.cbr;

import static com.predic8.membrane.core.util.SynchronizedXPathFactory.newXPath;


import java.util.*;

import javax.xml.stream.*;
import javax.xml.xpath.XPathConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.InputSource;

import com.predic8.membrane.core.config.GenericComplexElement;
import com.predic8.membrane.core.exchange.Exchange;
import com.predic8.membrane.core.http.Request;
import com.predic8.membrane.core.interceptor.AbstractInterceptor;
import com.predic8.membrane.core.interceptor.Outcome;

public class XPathCBRInterceptor extends AbstractInterceptor {
	private static Log log = LogFactory.getLog(XPathCBRInterceptor.class.getName());
	
	private RouteProvider routeProvider = new DefaultRouteProvider();
	private Map<String, String> namespaces;
	
	public XPathCBRInterceptor() {
		name = "Content Based Router";
	}
	
	@Override
	public Outcome handleRequest(Exchange exc) throws Exception {
		if (exc.getRequest().isBodyEmpty()) {
			return Outcome.CONTINUE;
		}
		
		Case r = findRoute(exc.getRequest());		
		if (r == null) {
			return Outcome.CONTINUE;
		}
		log.debug("match found for {"+r.getxPath()+"} routing to {"+ r.getUrl() + "}");
		
		updateDestination(exc, r);
		return Outcome.CONTINUE;
	}

	private void updateDestination(Exchange exc, Case r) {
		exc.setOriginalRequestUri(r.getUrl());		
		exc.getDestinations().clear();
		exc.getDestinations().add(r.getUrl());
	}

	private Case findRoute(Request request) throws Exception {
		for (Case r : routeProvider.getRoutes()) {
			//TODO getBodyAsStream creates ByteArray each call. That could be a performance issue. Using BufferedInputStream did't work, because stream got closed.
			if ( (Boolean) newXPath(namespaces).evaluate(r.getxPath(), new InputSource(request.getBodyAsStream()), XPathConstants.BOOLEAN) ) 
				return r;
			log.debug("no match found for xpath {"+r.getxPath()+"}");
		}			
		return null;			
	}

	public RouteProvider getRouteProvider() {
		return routeProvider;
	}

	public void setRouteProvider(RouteProvider routeProvider) {
		this.routeProvider = routeProvider;
	}

	public Map<String, String> getNamespaces() {
		return namespaces;
	}

	public void setNamespaces(Map<String, String> namespaces) {
		this.namespaces = namespaces;
	}

	@Override
	protected void writeInterceptor(XMLStreamWriter out)
			throws XMLStreamException {
		
		out.writeStartElement("switch");
		
		for (Case r : routeProvider.getRoutes()) {
			r.write(out);
		}
		
		out.writeEndElement();
	}
		
	@Override
	protected void parseChildren(XMLStreamReader token, String child)
			throws Exception {
		if (token.getLocalName().equals("case")) {
			Case r = new Case();
			r.parse(token);
			routeProvider.getRoutes().add(r);
		} else {
			super.parseChildren(token, child);
		}	
	}
}
