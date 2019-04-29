package org.kivy.android.launcher;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import binder24.com.hydrostar.BuildConfig;
import java.io.File;
import java.util.Arrays;
import org.renpy.android.ResourceManager;

public class ProjectChooser extends Activity implements OnItemClickListener {
    ResourceManager resourceManager;
    String urlScheme;

    public void onStart() {
        int i = 0;
        super.onStart();
        this.resourceManager = new ResourceManager(this);
        this.urlScheme = this.resourceManager.getString("urlScheme");
        setTitle(this.resourceManager.getString("appName"));
        File dir = new File(Environment.getExternalStorageDirectory(), this.urlScheme);
        File[] entries = dir.listFiles();
        if (entries == null) {
            entries = new File[0];
        }
        Arrays.sort(entries);
        ProjectAdapter projectAdapter = new ProjectAdapter(this);
        int length = entries.length;
        while (i < length) {
            Project p = Project.scanDirectory(entries[i]);
            if (p != null) {
                projectAdapter.add(p);
            }
            i++;
        }
        if (projectAdapter.getCount() != 0) {
            View v = this.resourceManager.inflateView("project_chooser");
            ListView l = (ListView) this.resourceManager.getViewById(v, "projectList");
            l.setAdapter(projectAdapter);
            l.setOnItemClickListener(this);
            setContentView(v);
            return;
        }
        v = this.resourceManager.inflateView("project_empty");
        ((TextView) this.resourceManager.getViewById(v, "emptyText")).setText("No projects are available to launch. Please place a project into " + dir + " and restart this application. Press the back button to exit.");
        setContentView(v);
    }

    public void onItemClick(AdapterView parent, View view, int position, long id) {
        Intent intent = new Intent("org.kivy.LAUNCH", Uri.fromParts(this.urlScheme, ((Project) parent.getItemAtPosition(position)).dir, BuildConfig.FLAVOR));
        intent.setClassName(getPackageName(), "org.kivy.android.PythonActivity");
        startActivity(intent);
        finish();
    }
}
