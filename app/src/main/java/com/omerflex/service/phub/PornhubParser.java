package com.omerflex.service.phub;

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
	public static ArrayList<Movie> getGeneralList(String html, Movie movie)
	{
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

		try
		{
			//Sometime json include invalid values got 'Unterminated string at character'
			json = MyUtils.removeBackSlash(json);
			
			String json_split = json.replaceAll("(?is)<iframe src=\"(.+?)</iframe>","")
				                    .replaceAll("(?is)<span class=\"(.+?)</span>","")
                                    .replaceAll("(?is)<a href=\"(.+?)</a>","") + "}";
			
			flashvars_jsonObject = new JSONObject(json_split);

			list = OfFlashvars(flashvars_jsonObject, movie);
			
		} catch(JSONException e)
		{
			e.printStackTrace();
		}
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
			for(int i = 0; i<mediaDefinitions.length(); i++)
			{
				JSONObject media = mediaDefinitions.getJSONObject(i);
				
				if(media.getString("format").equals("mp4"))
				{
					md_videoUrl = media.getString("videoUrl");
				}
			}
			
			
			if(md_videoUrl != null || !md_videoUrl.isEmpty())
			{
				String videoUrl_json = HttpRetriever.retrieve(md_videoUrl);
				
				JSONArray videoUrl_jsonArray = new JSONArray(videoUrl_json);
				
				for(int i = 0; i<videoUrl_jsonArray.length(); i++)
				{
					JSONObject videoUrl_jsonObject = videoUrl_jsonArray.getJSONObject(i);
					
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
//						list.add(new General(format, videoUrl, quality));
					}
				}
			}
		}
		catch (JSONException e)
		{
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
