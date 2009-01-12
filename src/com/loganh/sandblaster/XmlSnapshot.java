package com.loganh.sandblaster;

import java.io.*;
import java.util.*;

import android.content.Context;
import android.graphics.Color;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;


abstract public class XmlSnapshot {

  static final public int PACK_RUN_LENGTH = 64;

  // TODO: put a schema somewhere
  static public final String NS = "http://sandblaster.googlecode.com/svn/trunk/";

  static private void writeElementProductSet(Element.ProductSet productSet, XmlSerializer serializer) throws IOException {
    for (int i = 0; i < productSet.products.length; i++) {
      serializer.startTag(NS, "element-product")
          .attribute(NS, "product", productSet.products[i].name)
          .attribute(NS, "weight", Float.toString(productSet.weights[i]))
          .endTag(NS, "element-product");
    }
  }

  static private void writeElementTransmutation(Element.Transmutation transmutation, XmlSerializer serializer) throws IOException {
    serializer.startTag(NS, "element-transform")
        .attribute(NS, "subject", transmutation.target.name)
        .attribute(NS, "probability", Float.toString(transmutation.probability));
    if (transmutation.product != null) {
      writeElementProductSet(transmutation.product, serializer);
    }
    serializer.endTag(NS, "element-transform");
  }

  static private void writePackedRow(XmlSerializer serializer, int x, int y, String pack) throws IOException {
    int last = pack.length();
    while (last-- > 0) {
      if (pack.charAt(last) != '.') {
        break;
      }
    }
    serializer.startTag(NS, "particle-sequence")
        .attribute(NS, "x", Integer.toString(x))
        .attribute(NS, "y", Integer.toString(y))
        .text(pack.substring(0, last + 1))
        .endTag(NS, "particle-sequence");
  }

  static private String colorComponentToString(int component) {
    String result = Integer.toHexString(component);
    if (result.length() < 2) {
      return "0" + result;
    }
    return result;
  }

  static private String colorToString(int color) {
    return new StringBuffer("#")
        .append(colorComponentToString(Color.red(color)))
        .append(colorComponentToString(Color.green(color)))
        .append(colorComponentToString(Color.blue(color)))
        .toString();
  }

  static public void write(SandBox sandbox, Writer writer) throws IOException {
    int h = sandbox.getHeight();
    int w = sandbox.getWidth();
    XmlSerializer serializer = Xml.newSerializer();

    serializer.setOutput(writer);
    serializer.startDocument(null, null);
    serializer.setPrefix("sand", NS);
    serializer.startTag(NS, "sandbox")
        .attribute(NS, "width", Integer.toString(w))
        .attribute(NS, "height", Integer.toString(h))
        .attribute(NS, "paused", sandbox.playing ? "false" : "true");

    serializer.startTag(NS, "packed-sandbox").text(sandbox.pack()).endTag(NS, "packed-sandbox");

    serializer.startTag(NS, "element-set");
    for (Element element : sandbox.elementTable.elements) {
      serializer.startTag(NS, "element")
          .attribute(NS, "name", element.name)
          .attribute(NS, "id", "" + element.id)
          .attribute(NS, "color", colorToString(element.color))
          .attribute(NS, "drawable", element.drawable ? "true" : "false")
          .attribute(NS, "mobile", element.mobile ? "true" : "false")
          .attribute(NS, "density", Float.toString(element.density))
          .attribute(NS, "viscosity", Float.toString(element.viscosity));
      if (element.decayProbability > 0) {
        serializer.startTag(NS, "element-decay")
            .attribute(NS, "probability", Float.toString(element.decayProbability))
            .attribute(NS, "lifetime", Integer.toString(element.lifetime));
        if (element.decayProducts != null) {
          writeElementProductSet(element.decayProducts, serializer);
        }
        serializer.endTag(NS, "element-decay");
      }
      for (Element.Transmutation transmutation : sandbox.elementTable.getTransmutations(element)) {
        writeElementTransmutation(transmutation, serializer);
      }
      serializer.endTag(NS, "element");
    }
    serializer.endTag(NS, "element-set");

    for (SandBox.Source source : sandbox.getSources()) {
      if (source.element != null) {
        serializer.startTag(NS, "source")
            .attribute(NS, "x", Integer.toString(source.x))
            .attribute(NS, "y", Integer.toString(source.y))
            .attribute(NS, "element", source.element.toString().toLowerCase());
      }
    }

    for (int y = 0; y < h; y++) {
      int runStart = -1;
      StringBuffer pack = new StringBuffer();
      for (int x = 0; x < w; x++) {
        Element e = sandbox.elements[x][y];
        if (e != null) {
          if (runStart == -1) {
            runStart = x;
          }
          pack.append((char) ((int) 'A' + e.ordinal));
        } else if(runStart != -1) {
          pack.append('.');
        }
        if (pack.length() >= PACK_RUN_LENGTH) {
          writePackedRow(serializer, runStart, y, pack.toString());
          runStart = -1;
          pack = new StringBuffer();
        }
      }
      if (runStart != -1) {
        writePackedRow(serializer, runStart, y, pack.toString());
      }
    }

    serializer.endDocument();

    Log.i("testing pack/unpack");
    SandBox copy;
    try {
      copy = SandBox.unpack(sandbox.pack());
    } catch (IOException ex) {
      Log.e("error packing or unpacking sandbox", ex);
      return;
    }
    if (sandbox.equals(copy)) {
      Log.i("copy worked!");
    } else {
      Log.e("copy failed");
    }
  }

  static private ElementTable loadDefaultElementTable(Context context) throws IOException, XmlPullParserException {
    InputStream stream = context.getAssets().open("snapshot_new.xml");
    SandBox sandbox = XmlSnapshot.read(new InputStreamReader(stream), context);
    return sandbox.elementTable;
  }

  static private boolean getBooleanAttribute(XmlPullParser parser, String name, boolean defaultValue) {
    String value = parser.getAttributeValue(NS, name);
    if (value == null) {
      return defaultValue;
    }
    return value.toLowerCase().equals("true");
  }

  static private float getFloatAttribute(XmlPullParser parser, String name, float defaultValue) {
    String value = parser.getAttributeValue(NS, name);
    if (value == null) {
      return defaultValue;
    }
    return Float.parseFloat(value);
  }

  static private int getIntegerAttribute(XmlPullParser parser, String name, int defaultValue) {
    String value = parser.getAttributeValue(NS, name);
    if (value == null) {
      return defaultValue;
    }
    return Integer.parseInt(value);
  }

  static private int getColorAttribute(XmlPullParser parser, String name, int defaultValue) {
    String value = parser.getAttributeValue(NS, name);
    if (value == null) {
      return defaultValue;
    }
    return Color.parseColor(value);
  }

  static private class ElementProductSet {
    List<String> names;
    List<Float> weights;

    public ElementProductSet() {
      names = new ArrayList<String>();
      weights = new ArrayList<Float>();
    }

    public Element.ProductSet resolve(ElementTable table) {
      Element[] elements = new Element[names.size()];
      float[] wts = new float[names.size()];
      for (int i = 0; i < elements.length; i++) {
        elements[i] = table.resolve(names.get(i));
        wts[i] = weights.get(i);
      }
      return new Element.ProductSet(elements, wts);
    }
  }

  static private class ElementTransmutation {
    public String subject;
    public float probability;
    ElementProductSet productSet;

    public ElementTransmutation(String subject, float probability) {
      this.subject = subject;
      this.probability = probability;
      productSet = new ElementProductSet();
    }

    public Element.Transmutation resolve(ElementTable table) {
      return new Element.Transmutation(table.resolve(subject), probability, productSet.resolve(table));
    }
  }

  static private class ElementData {
    public Element element;
    List<ElementTransmutation> transmutations;
    ElementProductSet decayProductSet;

    public ElementData(Element element) {
      this.element = element;
      transmutations = new ArrayList<ElementTransmutation>();
    }
  }

  static public SandBox read(Reader reader, Context context) throws IOException, XmlPullParserException {
    SandBox sandbox = null;
    SandBox packbox = null;
    XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
    parser.setInput(reader);
    int sequenceX = -1;
    int sequenceY = -1;
    int eventType = parser.getEventType();
    boolean inSequence = false;
    boolean inPack = false;
    List<ElementData> elements = null;
    ElementData currentElement = null;
    ElementProductSet currentProductSet = null;
    ElementTable elementTable = null;

    while (eventType != XmlPullParser.END_DOCUMENT) {
      if (eventType == XmlPullParser.START_TAG) {
        if (parser.getName().equals("sandbox")) {
          int width = getIntegerAttribute(parser, "width", 0);
          int height = getIntegerAttribute(parser, "height", 0);
          if (width == 0 || height == 0) {
            return null;
          }
          Log.i("create sandbox: {0} x {1}", width, height);
          sandbox = new SandBox(width, height);
          sandbox.playing = !getBooleanAttribute(parser, "paused", true);
        } else if (parser.getName().equals("default-element-set")) {
          elementTable = loadDefaultElementTable(context);
        } else if (parser.getName().equals("element-set")) {
          elements = new ArrayList<ElementData>();
        } else if (parser.getName().equals("element")) {
          String name = parser.getAttributeValue(NS, "name");
          char id = parser.getAttributeValue(NS, "id").charAt(0);
          int color = getColorAttribute(parser, "color", Color.WHITE);
          boolean drawable = getBooleanAttribute(parser, "drawable", false);
          boolean mobile = getBooleanAttribute(parser, "mobile", true);
          float density = getFloatAttribute(parser, "density", 0);
          float viscosity = getFloatAttribute(parser, "viscosity", 1);
          currentElement = new ElementData(new Element(name, id, color, drawable, mobile, density, viscosity));
          elements.add(currentElement);
        } else if (parser.getName().equals("element-transform")) {
          String subject = parser.getAttributeValue(NS, "subject");
          float probability = getFloatAttribute(parser, "probability", 1);
          ElementTransmutation transmutation = new ElementTransmutation(subject, probability);
          currentProductSet = transmutation.productSet;
          currentElement.transmutations.add(transmutation);
          String product = parser.getAttributeValue(NS, "product");
          if (product != null) {
            currentProductSet.names.add(product);
            currentProductSet.weights.add(1f);
          }
        } else if (parser.getName().equals("element-decay")) {
          currentElement.element.decayProbability = getFloatAttribute(parser, "probability", 0);
          currentElement.element.lifetime = getIntegerAttribute(parser, "lifetime", 0);
          currentElement.decayProductSet = new ElementProductSet();
          currentProductSet = currentElement.decayProductSet;
          String product = parser.getAttributeValue(NS, "product");
          if (product != null) {
            currentProductSet.names.add(product);
            currentProductSet.weights.add(1f);
          }
        } else if (parser.getName().equals("element-product")) {
          currentProductSet.names.add(parser.getAttributeValue(NS, "product"));
          currentProductSet.weights.add(getFloatAttribute(parser, "weight", 1));
        } else if (parser.getName().equals("source") || parser.getName().equals("particle")) {
          int x = Integer.parseInt(parser.getAttributeValue(null, "x"));
          int y = Integer.parseInt(parser.getAttributeValue(null, "y"));
          Element element = elementTable.resolve(parser.getAttributeValue(null, "element"));
          if (parser.getName().equals("source")) {
            sandbox.addSource(element, x, y);
          } else {
            sandbox.setParticle(x, y, element);
          }
        } else if (parser.getName().equals("particle-sequence")) {
          inSequence = true;
          sequenceX = Integer.parseInt(parser.getAttributeValue(null, "x"));
          sequenceY = Integer.parseInt(parser.getAttributeValue(null, "y"));
        } else if (parser.getName().equals("packed-sandbox")) {
          inPack = true;
        }
      } else if (eventType == XmlPullParser.END_TAG) {
        if (parser.getName().equals("element-set")) {
          Element[] es = new Element[elements.size()];
          for (int i = 0; i < es.length; i++) {
            es[i] = elements.get(i).element;
          }
          elementTable = new ElementTable(es);
          for (int i = 0; i < es.length; i++) {
            for (ElementTransmutation et : elements.get(i).transmutations) {
              elementTable.addTransmutation(es[i], et.resolve(elementTable));
            }
            if (elements.get(i).decayProductSet != null) {
              es[i].decayProducts = elements.get(i).decayProductSet.resolve(elementTable);
            }
          }
        } else if (parser.getName().equals("element")) {
          currentElement = null;
        } else if (parser.getName().equals("element-transform")) {
          currentProductSet = null;
        } else if (parser.getName().equals("particle-sequence")) {
          inSequence = false;
        } else if (parser.getName().equals("packed-sandbox")) {
          inPack = false;
        }
      } else if (eventType == XmlPullParser.TEXT) {
        if (inSequence) {
          String pack = parser.getText().trim();
          for (int i = 0; i < pack.length(); i++) {
            sandbox.setParticle(sequenceX + i, sequenceY, elementTable.resolve(pack.charAt(i)));
          }
        } else if (inPack) {
          try {
            packbox = SandBox.unpack(parser.getText());
          } catch (IOException ex) {
            Log.e("error unpacking sandbox", ex);
          }
        }
      }
      eventType = parser.next();
    }
    if (sandbox != null) {
      sandbox.elementTable = elementTable;
    }
    return sandbox;
  }

  static public SandBox read_1_5(SandBox sandbox, Reader reader) throws IOException, XmlPullParserException {
    XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
    parser.setInput(reader);
    int sequenceX = -1;
    int sequenceY = -1;
    int eventType = parser.getEventType();
    boolean inSequence = false;
    while (eventType != XmlPullParser.END_DOCUMENT) {
      if (eventType == XmlPullParser.START_TAG) {
        if (parser.getName().equals("source") || parser.getName().equals("particle")) {
          int x = Integer.parseInt(parser.getAttributeValue(null, "x"));
          int y = Integer.parseInt(parser.getAttributeValue(null, "y"));
          Element element = sandbox.elementTable.resolve(parser.getAttributeValue(null, "element"));
          if (parser.getName().equals("source")) {
            sandbox.addSource(element, x, y);
          } else {
            sandbox.setParticle(x, y, element);
          }
        } else if (parser.getName().equals("particle-sequence")) {
          inSequence = true;
          sequenceX = Integer.parseInt(parser.getAttributeValue(null, "x"));
          sequenceY = Integer.parseInt(parser.getAttributeValue(null, "y"));
        }
      } else if (eventType == XmlPullParser.END_TAG) {
        if (parser.getName().equals("particle-sequence")) {
          inSequence = false;
        }
      } else if (eventType == XmlPullParser.TEXT) {
        if (inSequence) {
          String pack = parser.getText().trim();
          for (int i = 0; i < pack.length(); i++) {
            sandbox.setParticle(sequenceX + i, sequenceY, sandbox.elementTable.resolve(pack.charAt(i)));
          }
        }
      }
      eventType = parser.next();
    }
    return sandbox;
  }

  static public SandBox read_1_5(SandBox sandbox, String name, Context context) throws IOException, XmlPullParserException {
    return read_1_5(sandbox, new InputStreamReader(context.openFileInput(name + Snapshot.SNAPSHOT_EXTENSION)));
  }

}
