package org.edu_sharing.xoai;

import javax.xml.transform.*;
import java.util.Properties;

public class LomTransformer extends Transformer {

    @Override
    public void transform(Source xmlSource, Result outputTarget) throws TransformerException {
        System.out.println(xmlSource);
    }

    @Override
    public void setParameter(String name, Object value) {

    }

    @Override
    public Object getParameter(String name) {
        return null;
    }

    @Override
    public void clearParameters() {

    }

    @Override
    public void setURIResolver(URIResolver resolver) {

    }

    @Override
    public URIResolver getURIResolver() {
        return null;
    }

    @Override
    public void setOutputProperties(Properties oformat) {

    }

    @Override
    public Properties getOutputProperties() {
        return null;
    }

    @Override
    public void setOutputProperty(String name, String value) throws IllegalArgumentException {

    }

    @Override
    public String getOutputProperty(String name) throws IllegalArgumentException {
        return null;
    }

    @Override
    public void setErrorListener(ErrorListener listener) throws IllegalArgumentException {

    }

    @Override
    public ErrorListener getErrorListener() {
        return null;
    }
}
