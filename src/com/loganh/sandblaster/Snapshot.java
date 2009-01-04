package com.loganh.sandblaster;

import java.io.*;
import java.util.*;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Xml;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class Snapshot implements Comparable<Snapshot> {

  // Result codes.
  static final public int LOAD_SNAPSHOT_RESULT = 1;
  static final public int SAVE_SNAPSHOT_RESULT = 2;

  // File extensions
  static final public String SNAPSHOT_EXTENSION = ".snapshot";
  static final public String THUMBNAIL_EXTENSION = ".thumbnail";

  // Dimensions
  static final public int THUMBNAIL_WIDTH = 60;
  static final public int THUMBNAIL_HEIGHT = 60;

  // XML packing
  static final public int PACK_RUN_LENGTH = 64;

  // TODO: put a schema somewhere
  public static final String NS = "http://sandblaster.googlecode.com/svn/trunk/";

  public String name;
  public SandBox sandbox;
  private boolean loaded;

  public Snapshot(SandBox sandbox) {
    this.sandbox = sandbox;
    loaded = sandbox != null;
  }

  public Snapshot(String name) {
    this.name = name;
  }

  public Snapshot(String name, Context context) throws IOException {
    this(name);
    load(context);
  }

  public void load(Context context) throws IOException {
    Reader reader = new InputStreamReader(context.openFileInput(name + SNAPSHOT_EXTENSION));
    try {
      sandbox = read(reader);
      this.name = name;
      loaded = true;
      Log.i("loaded from {0}", name);
    } catch (XmlPullParserException ex) {
      throw new IOException("parse error");
    }
  }

  public void save(Context context) throws IOException {
    if (name == null) {
      name = "Autosave";
    }
    write(sandbox, new OutputStreamWriter(context.openFileOutput(name + SNAPSHOT_EXTENSION, 0)));
    saveThumbnail(context);
  }

  public Bitmap render() {
    SandBoxRenderer.setPixels(sandbox);
    return sandbox.bitmap;
  }

  private Bitmap saveThumbnail(Context context) throws IOException {
    Bitmap bitmap = Bitmap.createScaledBitmap(render(), THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, true);
    OutputStream stream = context.openFileOutput(name + THUMBNAIL_EXTENSION, 0);
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
    stream.close();
    return bitmap;
  }

  private Bitmap getThumbnail(Context context) throws IOException {
    try {
      return BitmapFactory.decodeStream(context.openFileInput(name + THUMBNAIL_EXTENSION));
    } catch (IOException ex) {
      Log.e("Unable to load thumbnail for " + name, ex);
    }
    if (!loaded) {
      load(context);
    }
    return saveThumbnail(context);
  }

  public int compareTo(Snapshot other) {
    if (name == null) {
      if (other.name == null) {
        return 0;
      }
      return -1;
    }
    if (other.name == null) {
      return 1;
    }
    return name.toLowerCase().compareTo(other.name.toLowerCase());
  }

  static public Set<Snapshot> getSnapshots(Context context) {
    Set<Snapshot> snapshots = new TreeSet();
    for (String filename : context.fileList()) {
      if (filename.endsWith(SNAPSHOT_EXTENSION)) {
        String name = filename.substring(0, filename.length() - SNAPSHOT_EXTENSION.length());
        snapshots.add(new Snapshot(name));
      }
    }
    return snapshots;
  }

  static public void deleteAll(Context context) {
    for (Snapshot snapshot : getSnapshots(context)) {
      context.deleteFile(snapshot.name + SNAPSHOT_EXTENSION);
      context.deleteFile(snapshot.name + THUMBNAIL_EXTENSION);
    }
  }

  static public void write(SandBox sandbox, Writer writer) throws IOException {
    int h = sandbox.getHeight();
    int w = sandbox.getWidth();
    XmlSerializer serializer = Xml.newSerializer();

    serializer.setOutput(writer);
    serializer.startDocument(null, null);
    serializer.startTag(null, "sandbox")
        .attribute(null, "width", Integer.toString(w))
        .attribute(null, "height", Integer.toString(h));

    for (SandBox.Source source : sandbox.getSources()) {
      serializer.startTag(null, "source")
          .attribute(null, "x", Integer.toString(source.x))
          .attribute(null, "y", Integer.toString(source.y))
          .attribute(null, "element", source.element.toString().toLowerCase());
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
          pack.append((char) ((int) 'A' + e.index));
          /*
          serializer.startTag(null, "particle")
              .attribute(null, "x", Integer.toString(x))
              .attribute(null, "y", Integer.toString(y))
              .attribute(null, "element", e.toString().toLowerCase())
              .endTag(null, "particle");
              */
        } else if(runStart != -1) {
          pack.append('.');
        }
        if (pack.length() >= PACK_RUN_LENGTH) {
          emitPackedRow(serializer, runStart, y, pack.toString());
          runStart = -1;
          pack = new StringBuffer();
        }
      }
      if (runStart != -1) {
        emitPackedRow(serializer, runStart, y, pack.toString());
      }
    }

    serializer.endDocument();
  }

  static private void emitPackedRow(XmlSerializer serializer, int x, int y, String pack) throws IOException {
    int last = pack.length();
    while (last-- > 0) {
      if (pack.charAt(last) != '.') {
        break;
      }
    }
    serializer.startTag(null, "particle-sequence")
        .attribute(null, "x", Integer.toString(x))
        .attribute(null, "y", Integer.toString(y))
        .text(pack.substring(0, last + 1))
        .endTag(null, "particle-sequence");
  }

  static public SandBox read(Reader reader) throws IOException, XmlPullParserException {
    SandBox sandbox = null;
    XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
    parser.setInput(reader);
    int sequenceX = -1;
    int sequenceY = -1;
    int eventType = parser.getEventType();
    boolean inSequence = false;
    while (eventType != XmlPullParser.END_DOCUMENT) {
      if (eventType == XmlPullParser.START_TAG) {
        if (parser.getName().equals("sandbox")) {
          int width = Integer.parseInt(parser.getAttributeValue(null, "width"));
          int height = Integer.parseInt(parser.getAttributeValue(null, "height"));
          sandbox = new SandBox(width, height);
        } else if (parser.getName().equals("source") || parser.getName().equals("particle")) {
          int x = Integer.parseInt(parser.getAttributeValue(null, "x"));
          int y = Integer.parseInt(parser.getAttributeValue(null, "y"));
          Element element = Element.valueOf(parser.getAttributeValue(null, "element").toUpperCase());
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
            if (pack.charAt(i) != '.') {
              for (Element e : Element.values()) {
                if ((char) ((int) 'A' + e.ordinal()) == pack.charAt(i)) {
                  sandbox.setParticle(sequenceX + i, sequenceY, e);
                  break;
                }
              }
            }
          }
        }
      }
      eventType = parser.next();
    }
    return sandbox;
  }

  static public class PickActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.snapshot_pick);

      if (getIntent().getType().equals("application/x-sandblaster-saveable")) {
        final Button button = (Button) findViewById(R.id.save_button);
        final EditText text = (EditText) findViewById(R.id.save_input);
        text.setOnKeyListener(new View.OnKeyListener() {
              public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER && !event.isShiftPressed()) {
                  if (event.getAction() == KeyEvent.ACTION_DOWN && button.isEnabled()) {
                    finish(text.getText().toString());
                  }
                  return true;
                }
                return false;
              }
            });
        text.addTextChangedListener(new TextWatcher() {
              public void afterTextChanged(Editable s) {
                button.setEnabled(s.length() > 0 && !s.toString().contains("/"));
              }

              public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
              public void onTextChanged(CharSequence s, int start, int before, int count) { }
            });
      } else {
        // Lose the save input row when loading.
        LinearLayout layout = (LinearLayout) findViewById(R.id.picker_layout);
        layout.removeViewAt(0);
      }

      ListView list = (ListView) findViewById(R.id.snapshot_list);
      list.setAdapter(new SnapshotListAdapter(this));
      list.setOnItemClickListener(new ListView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
              Snapshot snapshot = (Snapshot) parent.getItemAtPosition(position);
              finish(snapshot.name);
            }
          });
    }

    public void finish(String name) {
      int resultCode = getIntent().getType().equals("application/x-sandblaster-loadable")
          ? LOAD_SNAPSHOT_RESULT : SAVE_SNAPSHOT_RESULT;
      Intent intent = new Intent(Intent.ACTION_PICK, Uri.fromParts("sandblaster", name, null));
      setResult(resultCode, intent);
      finish();
    }
  }

  static private class SnapshotListAdapter extends ArrayAdapter<Snapshot> {

    public SnapshotListAdapter(Context context) {
      super(context, 0, getSnapshots(context).toArray(new Snapshot[]{}));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      Snapshot snapshot = getItem(position);
      return new SnapshotView(getContext(), getItem(position));
    }
  }

  static private class SnapshotView extends LinearLayout {

    private Snapshot snapshot;

    public SnapshotView(Context context, Snapshot snapshot) {
      super(context);
      this.snapshot = snapshot;
      LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      inflater.inflate(R.layout.snapshot, this);
      ImageView image = (ImageView) findViewById(R.id.snapshot_thumbnail);
      try {
        image.setImageBitmap(snapshot.getThumbnail(context));
      } catch (IOException ex) {
        Log.e("failed to get or generate thumbnail for " + snapshot.name, ex);
      }
      TextView text = (TextView) findViewById(R.id.snapshot_title);
      text.setText(snapshot.name);
    }
  }
}
