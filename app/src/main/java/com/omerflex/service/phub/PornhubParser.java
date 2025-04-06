package com.omerflex.service.phub;

import android.util.Log;

import com.omerflex.entity.Movie;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PornhubParser
{
	static String TAG = "PornhubParser";
	public static ArrayList<Movie> getGeneralList(String html, Movie movie)
	{
		// Log.d(TAG, "getGeneralList: ");
		ArrayList<Movie> list = new ArrayList<>();
		
		Pattern p_var;
		Matcher matcher;
		
		String json = null;
		JSONObject flashvars_jsonObject;
		//JSONArray qualityItems_jsonArray;
		
		p_var = Pattern.compile("var\\s+flashvars_\\d+\\s*=\\s*(.+?)\\};"); 
		matcher = p_var.matcher(html);
		while( matcher.find() )
		{
			json = matcher.group(1);
		}
		// Log.d(TAG, "getGeneralList: json: "+json);
		try
		{
			//Sometime json include invalid values got 'Unterminated string at character'
			json = MyUtils.removeBackSlash(json);
			// Log.d(TAG, "getGeneralList: 3");
			String json_split = json.replaceAll("(?is)<iframe src=\"(.+?)</iframe>","")
				                    .replaceAll("(?is)<span class=\"(.+?)</span>","")
                                    .replaceAll("(?is)<a href=\"(.+?)</a>","") + "}";

			// Log.d(TAG, "getGeneralList: 4 json:"+ json_split);
			flashvars_jsonObject = new JSONObject(json_split);
			// Log.d(TAG, "getGeneralList: 5");

			list = OfFlashvars(flashvars_jsonObject, movie);
			// Log.d(TAG, "getGeneralList: list: "+ list);
		} catch(JSONException e)
		{
			Log.e(TAG, "getGeneralList: "+ e.getMessage() );
			e.printStackTrace();
		}
		// Log.d(TAG, "getGeneralList: 6");
		//Nothing var qualityItems anymore :)
//		if(list.isEmpty())
//		{
//			p_var = Pattern.compile("var\\s+qualityItems_\\d+\\s*=\\s*(.+?);");
//			
//			matcher = p_var.matcher(html);
//			while( matcher.find() )
//			{
//				json = matcher.group(1);
//			}
//
//			try
//			{
//				qualityItems_jsonArray = new JSONArray(json);
//
//				list = OfQualityItems(qualityItems_jsonArray);
//			} catch(JSONException e)
//			{
//				e.printStackTrace();
//			}
//			
//		}
		Collections.reverse(list);
		return list;
	}
	
	
	
	public static ArrayList<Movie> OfFlashvars(JSONObject jsonObject, Movie movie)
	{
		ArrayList<Movie> list = new ArrayList<>();
		
		try
		{
			JSONArray mediaDefinitions = jsonObject.getJSONArray("mediaDefinitions");
			String md_videoUrl = null;
			// Log.d(TAG, "OfFlashvars: 7");
			for(int i = 0; i<mediaDefinitions.length(); i++)
			{
				JSONObject media = mediaDefinitions.getJSONObject(i);

				// Log.d(TAG, "OfFlashvars: 8");
				if(media.getString("format").equals("mp4"))
				{
					md_videoUrl = media.getString("videoUrl");
				}
			}

			// Log.d(TAG, "OfFlashvars: 9");

			if(md_videoUrl != null || !md_videoUrl.isEmpty())
			{


				try {
					// Log.d(TAG, "OfFlashvars: 10 url: "+ md_videoUrl);
					String videoUrl_json = HttpRetriever.retrieve(md_videoUrl);
					// Log.d(TAG, "OfFlashvars: 11");

					JSONArray videoUrl_jsonArray = new JSONArray(videoUrl_json);
					// Log.d(TAG, "OfFlashvars: 11 json: "+ videoUrl_json);
					for(int i = 0; i<videoUrl_jsonArray.length(); i++)
					{
						JSONObject videoUrl_jsonObject = videoUrl_jsonArray.getJSONObject(i);

						// Log.d(TAG, "OfFlashvars: 12");
						String format = videoUrl_jsonObject.getString("format");
						String videoUrl = videoUrl_jsonObject.getString("videoUrl");
						String quality = videoUrl_jsonObject.getString("quality");

						if(!videoUrl.isEmpty())
						{
							Movie resolution = Movie.clone(movie);
							resolution.setState(Movie.VIDEO_STATE);
							resolution.setTitle(quality);
							resolution.setVideoUrl(videoUrl);
							list.add(resolution);
							// Log.d(TAG, "OfFlashvars: 12 videoUrl: " + resolution.toString() );
//						list.add(new General(format, videoUrl, quality));
						}
					}
				}catch (Exception e){
					Log.e(TAG, "OfFlashvars: 11 error: "+e.getMessage());
				}


			}
		}
		catch (JSONException e)
		{
			Log.e(TAG, "OfFlashvars: error: "+e.getMessage());
			e.printStackTrace();
		}
		return list;
	}
	
	//this is plan B if plan A(method OfFlashvars) failed =))) 
	
	//nothing var qualityItems anymore :)
//	public static List<General> OfQualityItems(JSONArray jsonArray)
//	{
//		List<General> list = new ArrayList<>();
//		
//		for(int i = 0; i<jsonArray.length(); i++)
//		{
//			try
//			{
//				JSONObject jsonObject = jsonArray.getJSONObject(i);
//				
//				String text = jsonObject.getString("text");
//				String url = jsonObject.getString("url");
//				
//				if(!url.isEmpty())
//				{
//					list.add(new General("MP4", url, text));
//				}
//				
//			}
//			catch (JSONException e)
//			{
//				e.printStackTrace();
//			}
//		}
//		
//		return list;
//	}
	
	
}
