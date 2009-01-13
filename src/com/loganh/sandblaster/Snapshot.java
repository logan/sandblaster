package com.loganh.sandblaster;

import java.io.*;
import java.util.*;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
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

import org.xmlpull.v1.XmlPullParserException;

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

  static final private String XML_PREFIX = "<?xml";

  public String name;
  public SandBox sandbox;
  public UndoStack undoStack;
  private boolean loaded;

  public Snapshot(SandBox sandbox) {
    this(sandbox, new UndoStack());
  }

  public Snapshot(SandBox sandbox, UndoStack undoStack) {
    this.sandbox = sandbox;
    this.undoStack = undoStack;
    loaded = sandbox != null;
  }

  public Snapshot(String name) {
    this.name = name;
  }

  public Snapshot(String name, Context context) throws IOException {
    this(name);
    load(context);
  }

  private boolean isXml(InputStream stream) throws IOException {
    Log.i("checking for xml header");
    if (!stream.markSupported()) {
      throw new IOException();
    }
    byte[] header = new byte[XML_PREFIX.length()];
    stream.mark(header.length);
    int nbytes = stream.read(header);
    if (nbytes == header.length) {
      for (int i = 0; i < nbytes; i++) {
        if (XML_PREFIX.charAt(i) != header[i]) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  public void load(Context context) throws IOException {
    InputStream stream = new BufferedInputStream(context.openFileInput(name + SNAPSHOT_EXTENSION));
    if (isXml(stream)) {
      Log.i("found xml snapshot");
      stream.reset();
      try {
        sandbox = XmlSnapshot.read(new InputStreamReader(stream), context);
        undoStack = new UndoStack();
        loaded = true;
        Log.i("loaded from {0}", name);
      } catch (XmlPullParserException ex) {
        throw new IOException("parse error");
      }
    } else {
      Log.i("found data stream snapshot");
      stream.reset();
      DataInputStream dataStream = new DataInputStream(stream);
      sandbox = SandBox.read(dataStream);
      undoStack = UndoStack.read(dataStream);
    }
  }

  public void save(Context context) throws IOException {
    if (name == null) {
      name = "Autosave";
    }
    DataOutputStream out = new DataOutputStream(new BufferedOutputStream(context.openFileOutput(name + SNAPSHOT_EXTENSION, 0)));
    synchronized (sandbox) {
      sandbox.write(out);
    }
    undoStack.write(out);
    out.close();
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
    Set<Snapshot> snapshots = new TreeSet<Snapshot>();
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
      return new SnapshotView(getContext(), getItem(position));
    }
  }

  static private class SnapshotView extends LinearLayout {

    public SnapshotView(Context context, Snapshot snapshot) {
      super(context);
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
