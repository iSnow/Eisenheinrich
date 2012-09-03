package net.krautchan.android;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import net.krautchan.android.helpers.FileHelpers;

import android.content.Context;
import android.os.Build;
import android.widget.Toast;

import com.osbcp.cssparser.CSSParser;
import com.osbcp.cssparser.Rule;

public class Styles {
	private List<Rule>  cssRules;
	private String 		styles;
	//private String		bodyBGCol;
	
	public Styles (Context context) {
		String css = null;
		String css2 = null;
		try {
			InputStream is = context.getAssets().open("style.css");
			BufferedReader r = new BufferedReader(new InputStreamReader(is));
			StringBuilder builder = new StringBuilder();
			String line;
			while ((line = r.readLine()) != null) {
				builder.append(line); 
			}
			r.close();
			r = null;
			css = builder.toString();
		} catch (IOException e) { 
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			File cssFile = FileHelpers.getSDFile("style.css");
			if ((null != cssFile) && (cssFile.exists())) {
				InputStream cssStream = new FileInputStream (cssFile);
				BufferedReader r = new BufferedReader(new InputStreamReader(cssStream));
				StringBuilder builder = new StringBuilder();
				String line;
				while ((line = r.readLine()) != null) {
					builder.append(line); 
				}
				r.close();
				r = null;
				css2 = css +"\n"+builder.toString();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (Build.VERSION.SDK_INT <= 9) {
			styles = css+"\n"+css2;
		} else {
			if (null != css2) {
				try {
					cssRules = CSSParser.parse(css2);
				} catch (Exception e) {
					try {
						cssRules = CSSParser.parse(css);
					} catch (Exception e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
				}
			} else {
				try {
					cssRules = CSSParser.parse(css);
				} catch (Exception e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
			}
			StringBuffer buf = new StringBuffer (16000);
			if (null == cssRules) {
				Toast.makeText(context, "CSS malformed", Toast.LENGTH_LONG).show();
			} else {
				for (Rule r: cssRules) {
					/*List<Selector> selectors = r.getSelectors();
					for (Selector s : selectors) {
						if (s.toString().equals("body")){
							List<PropertyValue> pvs = r.getPropertyValues();
							for (PropertyValue pv : pvs) {
								if (pv.getProperty().equals("background-color")) {
									this.bodyBGCol = pv.getValue();
								}
							}
						}
					}*/
					buf.append(r.toString());
				}
			}
			styles = buf.toString();
		}
	}
	
	public String getStyles() {
		return styles;
	}

}
