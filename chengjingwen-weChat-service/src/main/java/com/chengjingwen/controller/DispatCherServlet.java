package com.chengjingwen.controller;

import java.io.PrintWriter;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.chengjingwen.entity.TextMessage;
import com.chengjingwen.utils.CheckUtil;
import com.chengjingwen.utils.HttpClientUtil;
import com.chengjingwen.utils.XmlUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 微信对接步骤： 1.填写服务器地址 2.验证服务器地址是否正确 3.任何微信动作，都会以post请求通知到该地址
 * 
 * @author tangc
 *
 */
@Slf4j
@RestController
public class DispatCherServlet {

	/**
	 * 验证消息的确来自微信服务器
	 * https://mp.weixin.qq.com/wiki?t=resource/res_main&id=mp1421135319
	 * 1）将token、timestamp、nonce三个参数进行字典序排序 2）将三个参数字符串拼接成一个字符串进行sha1加密
	 * 3）开发者获得加密后的字符串可与signature对比，标识该请求来源于微信
	 * 
	 * @param signature
	 * @param timestamp
	 * @param nonce
	 * @param echostr
	 * @return
	 */
	@RequestMapping(value = "/dispatCherServlet", method = RequestMethod.GET)
	public String getDispatCherServlet(String signature, String timestamp, String nonce, String echostr) {
		boolean checkSignature = CheckUtil.checkSignature(signature, timestamp, nonce);
		if (!checkSignature) {
			return null;
		}
		return echostr;
	}

	/**
	 * 接收普通消息
	 * 
	 * 当普通微信用户向公众账号发消息时，微信服务器将POST消息的XML数据包到开发者填写的URL上。
	 * 
	 * 请注意：
	 * 
	 * 1、关于重试的消息排重，推荐使用msgid排重。
	 * 
	 * 2、微信服务器在五秒内收不到响应会断掉连接，并且重新发起请求，总共重试三次。假如服务器无法保证在五秒内处理并回复，
	 * 
	 * 可以直接回复空串，微信服务器不会对此作任何处理，并且不会发起重试。详情请见“发送消息-被动回复消息”。
	 * 
	 * 3、如果开发者需要对用户消息在5秒内立即做出回应，即使用“发送消息-被动回复消息”接口向用户被动回复消息时，可以在
	 * 
	 * 公众平台官网的开发者中心处设置消息加密。开启加密后，用户发来的消息和开发者回复的消息都会被加密（但开发者通过客服
	 * 
	 * 接口等API调用形式向用户发送消息，则不受影响）。关于消息加解密的详细说明，请见“发送消息-被动回复消息加解密说明”。
	 * 
	 * @param request
	 * @param response
	 * @param signature
	 * @param timestamp
	 * @param nonce
	 * @param echostr
	 * @throws Exception
	 */
	@RequestMapping(value = "/dispatCherServlet", method = RequestMethod.POST)
	public void getDispatCherServlet(HttpServletRequest request, HttpServletResponse response, String signature,
			String timestamp, String nonce, String echostr) throws Exception {
		request.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("UTF-8");
		/**
		 * 文本消息 <xml> <ToUserName>< ![CDATA[toUser] ]></ToUserName> <FromUserName><
		 * ![CDATA[fromUser] ]></FromUserName> <CreateTime>1348831860</CreateTime>
		 * <MsgType>< ![CDATA[text] ]></MsgType> <Content>< ![CDATA[this is a test]
		 * ]></Content> <MsgId>1234567890123456</MsgId> </xml> xml转换成map
		 */
		Map<String, String> result = XmlUtils.parseXml(request);
		String toUserName = result.get("ToUserName");
		String fromUserName = result.get("FromUserName");
		String msgType = result.get("MsgType");
		String content = result.get("Content");
		switch (msgType) {
		// 如果为文本消息
		case "text":
			String resultXml = null;
			PrintWriter out = response.getWriter();
			TextMessage textMessage = new TextMessage();
			textMessage.setToUserName(fromUserName);
			textMessage.setFromUserName(toUserName);
			textMessage.setCreateTime(System.currentTimeMillis());
			textMessage.setMsgType("text");
			// 判断文本信息值
			if (content.equals("唐成靖文")) {
				textMessage.setContent("唐成靖文❤迪丽热巴迪力木拉提");
				log.info("唐成靖文❤迪丽热巴迪力木拉提");
			} else {
				// 调用第三方智能机器人接口(青云客智能聊天机器人API) http://api.qingyunke.com/
				String resultApiStr = HttpClientUtil
						.doGet("http://api.qingyunke.com/api.php?key=free&appid=0&msg=" + content);
				@SuppressWarnings("static-access")
				// 将结果转化为json格式
				JSONObject jsonObject = new JSONObject().parseObject(resultApiStr);
				Integer status = jsonObject.getInteger("result");
				//判断结果的状态，result状态，0表示正常，其它数字表示错误
				if (status == 0) {
					String contentApi = jsonObject.getString("content");
					textMessage.setContent(contentApi);
					log.info(contentApi);
				}
			}
			// 转换为xml格式
			resultXml = XmlUtils.messageToXml(textMessage);
			out.print(resultXml);
			out.close();
			break;

		default:
			break;
		}
	}
}
