/*
 * 
 * Copyright 2014 Jules White
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.magnum.dataup;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.magnum.dataup.model.VideoStatus.VideoState;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class VideoController {

	private Map<Long, Video> videos = new HashMap<Long, Video>();
	private static final AtomicLong currentId = new AtomicLong(0L);
	// Initialize this member variable somewhere with 
    // videoDataMgr = VideoFileManager.get()
    //
    private VideoFileManager videoDataMgr;
    
    // You would need some Controller method to call this...
  	public void saveSomeVideo(Video video, MultipartFile videoData) throws IOException {
  		 videoDataMgr = VideoFileManager.get();
  	     videoDataMgr.saveVideoData(video, videoData.getInputStream());
  	}
  	
  	public void serveSomeVideo(Video v, HttpServletResponse response) throws IOException {
  	     // Of course, you would need to send some headers, etc. to the
  	     // client too!
  	     //  ...
  		 videoDataMgr = VideoFileManager.get();
  	     videoDataMgr.copyVideoData(v, response.getOutputStream());
  	}

	public Video save(Video entity) {
		checkAndSetId(entity);
		videos.put(entity.getId(), entity);
		return entity;
	}

	private void checkAndSetId(Video entity) {
		if (entity.getId() == 0) {
			entity.setId(currentId.incrementAndGet());
		}
	}

	private String getDataUrl(long videoId) {

		String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";

		return url;

	}

	private String getUrlBaseForLocalServer() {

		HttpServletRequest request =

		((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
				.getRequest();

		String base =

		"http://"
				+ request.getServerName()

				+ ((request.getServerPort() != 80) ? ":"
						+ request.getServerPort() : "");

		return base;

	}

	@RequestMapping(value = VideoSvcApi.VIDEO_SVC_PATH, method = RequestMethod.GET)
	public @ResponseBody
	Collection<Video> getVideoList() {
		return videos.values();
	}

	@RequestMapping(value = VideoSvcApi.VIDEO_SVC_PATH, method = RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video v) {
		save(v);
		v.setDataUrl(getDataUrl(v.getId()));
		videos.put(v.getId(), v);
		return v;
	}

	@RequestMapping(value = VideoSvcApi.VIDEO_DATA_PATH, method = RequestMethod.POST)
	public @ResponseBody
	VideoStatus sendVideoData(@PathVariable(VideoSvcApi.ID_PARAMETER) long id,
			@RequestParam(VideoSvcApi.DATA_PARAMETER) MultipartFile videoData,HttpServletResponse response) {
		try {
			if(videos.get(id)!=null){
			InputStream in = videoData.getInputStream();
			VideoFileManager.get().saveVideoData(videos.get(id), in);
			}
			else
				response.setStatus(404);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new VideoStatus(VideoState.READY);
	}
	@RequestMapping(value = VideoSvcApi.VIDEO_DATA_PATH, method = RequestMethod.GET)
	public void  getData(@PathVariable(VideoSvcApi.ID_PARAMETER)long id, HttpServletResponse response) throws IOException {
		if(videos.get(id)!=null){
		serveSomeVideo(videos.get(id), response);}
		else
			response.setStatus(404);
	}

}
