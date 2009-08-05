package com.loganh.sandblaster;

import java.util.EnumSet;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.PopupWindow;


public class ToolSelector extends PopupWindow {

  final static public int TOOL_COLUMNS = 4;

  private Toolbar toolbar;
  private Context context;

  public ToolSelector(Toolbar toolbar) {
    super(toolbar.getContext());
    this.toolbar = toolbar;
    context = toolbar.getContext();
    layout();
    showAsDropDown(toolbar);
  }

  private void layout() {
    GridView grid = new GridView(context);
    grid.setNumColumns(TOOL_COLUMNS);
    grid.setAdapter(new ToolAdapter());
    grid.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    setContentView(grid);
    setBackgroundDrawable(null);
    setWidth(toolbar.getWidth());
    setHeight(56);
  }

  public class ToolAdapter extends BaseAdapter {

    public View getView(int position, View convertView, ViewGroup parent) {
      final Pen.Tool tool = (Pen.Tool) getItem(position);
      if (tool == null) {
        return null;
      }

      ImageButton button = new ImageButton(context);
      button.setImageResource(toolbar.getIcon(tool));
      button.setLayoutParams(new GridView.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
      button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
              toolbar.selectTool(tool);
            }
          });
      return button;
    }

    public final int getCount() {
      return EnumSet.allOf(Pen.Tool.class).size();
    }

    public final Object getItem(int position) {
      for (Pen.Tool tool : EnumSet.allOf(Pen.Tool.class)) {
        if (tool.ordinal() == position) {
          return tool;
        }
      }
      return null;
    }

    public final long getItemId(int position) {
      return position;
    }
  }

}
