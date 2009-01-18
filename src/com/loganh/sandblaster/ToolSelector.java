package com.loganh.sandblaster;

import java.util.EnumSet;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.PopupWindow;


public class ToolSelector extends PopupWindow {

  final static public int TOOL_COLUMNS = 2;

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
    setWidth(200);
    setHeight(200);
  }

  public class ToolAdapter extends BaseAdapter {

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      final Pen.Tool tool = (Pen.Tool) getItem(position);
      if (tool == null) {
        return null;
      }

      TextView button = new TextView(context);
      button.setText(tool.toString());
      button.setLayoutParams(new GridView.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
      button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
              toolbar.selectTool(tool);
            }
          });
      return button;
    }

    @Override
    public final int getCount() {
      return EnumSet.allOf(Pen.Tool.class).size();
    }

    @Override
    public final Object getItem(int position) {
      for (Pen.Tool tool : EnumSet.allOf(Pen.Tool.class)) {
        if (tool.ordinal() == position) {
          return tool;
        }
      }
      return null;
    }

    @Override
    public final long getItemId(int position) {
      return position;
    }
  }

}
