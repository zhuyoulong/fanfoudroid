package com.ch_linghu.fanfoudroid.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.ch_linghu.fanfoudroid.R;
import com.ch_linghu.fanfoudroid.TwitterApplication;
import com.ch_linghu.fanfoudroid.data.db.StatusTable;
import com.ch_linghu.fanfoudroid.data.db.TwitterDatabase;
import com.ch_linghu.fanfoudroid.helper.Utils;
import com.ch_linghu.fanfoudroid.http.HttpException;
import com.ch_linghu.fanfoudroid.http.Response;
import com.ch_linghu.fanfoudroid.weibo.Photo;
import com.ch_linghu.fanfoudroid.weibo.RetweetDetails;
import com.ch_linghu.fanfoudroid.weibo.User;
import com.ch_linghu.fanfoudroid.weibo.Weibo;

public class FanStatus extends Object<FanStatus> implements BaseContent  {
    private static final String TAG = "FanStatus";
    private static final long serialVersionUID = 1608000492860584608L;

    private Date createdAt;
    private String id;
    private String text;
    private String source;
    private boolean isTruncated;
    private String inReplyToStatusId;
    private String inReplyToUserId;
    private boolean isFavorited;
    private String inReplyToScreenName;
    private double latitude = -1;
    private double longitude = -1;
    private String thumbnail_pic;
    private String bmiddle_pic;
    private String original_pic;
    private String photo_url;
    private RetweetDetails retweetDetails;
    private User user = null;
    private int statusType = -1;  // @see StatusTable#TYPE_*
    
    private boolean isDirty = false; // 是否被修改

    public FanStatus(Response res, Weibo weibo) throws HttpException {
        super(res);
        Element elem = res.asDocument().getDocumentElement();
        init(res, elem, weibo);
    }

    public FanStatus(Response res, Element elem, Weibo weibo) throws HttpException {
        super(res);
        init(res, elem, weibo);
    }
    
    public FanStatus(Response res)throws HttpException{
        super(res);
        JSONObject json=res.asJSONObject();
        try {
            id = json.getString("id");
            text = json.getString("text");
            source = json.getString("source");
            createdAt = parseDate(json.getString("created_at"), "EEE MMM dd HH:mm:ss z yyyy");

            inReplyToStatusId = getString("in_reply_to_status_id", json);
            inReplyToUserId = getString("in_reply_to_user_id", json);
            isFavorited = getBoolean("favorited", json);
//          System.out.println("json photo" + json.getJSONObject("photo"));
            if(!json.isNull("photo")) {
//              System.out.println("not null" + json.getJSONObject("photo"));
                Photo photo = new Photo(json.getJSONObject("photo"));
                thumbnail_pic = photo.getThumbnail_pic();
                bmiddle_pic = photo.getBmiddle_pic();
                original_pic = photo.getOriginal_pic();
            } else {
//              System.out.println("Null");
                thumbnail_pic = "";
                bmiddle_pic = "";
                original_pic = "";
            }
            if(!json.isNull("user"))
                user = new User(json.getJSONObject("user"));
                inReplyToScreenName=json.getString("in_reply_to_screen_name");
            if(!json.isNull("retweetDetails")){
                retweetDetails = new RetweetDetails(json.getJSONObject("retweetDetails"));
            }
        } catch (JSONException je) {
            throw new HttpException(je.getMessage() + ":" + json.toString(), je);
        }
        
    }
    
    /* modify by sycheng add some field*/
    public FanStatus(JSONObject json)throws HttpException, JSONException{
        id = json.getString("id");
        text = json.getString("text");
        source = json.getString("source");
        createdAt = parseDate(json.getString("created_at"), "EEE MMM dd HH:mm:ss z yyyy");

        isFavorited = getBoolean("favorited", json);
        isTruncated=getBoolean("truncated", json);
        
        inReplyToStatusId = getString("in_reply_to_status_id", json);
        inReplyToUserId = getString("in_reply_to_user_id", json);
        inReplyToScreenName=json.getString("in_reply_to_screen_name");
        if(!json.isNull("photo")) {
            Photo photo = new Photo(json.getJSONObject("photo"));
            thumbnail_pic = photo.getThumbnail_pic();
            bmiddle_pic = photo.getBmiddle_pic();
            original_pic = photo.getOriginal_pic();
        } else {
            thumbnail_pic = "";
            bmiddle_pic = "";
            original_pic = "";
        }
        user = new User(json.getJSONObject("user"));
    }
    
    public FanStatus(String str) throws HttpException, JSONException {
        // StatusStream uses this constructor
        super();
        JSONObject json = new JSONObject(str);
        id = json.getString("id");
        text = json.getString("text");
        source = json.getString("source");
        createdAt = parseDate(json.getString("created_at"), "EEE MMM dd HH:mm:ss z yyyy");

        inReplyToStatusId = getString("in_reply_to_status_id", json);
        inReplyToUserId = getString("in_reply_to_user_id", json);
        isFavorited = getBoolean("favorited", json);
        if(!json.isNull("photo")) {
            Photo photo = new Photo(json.getJSONObject("photo"));
            thumbnail_pic = photo.getThumbnail_pic();
            bmiddle_pic = photo.getBmiddle_pic();
            original_pic = photo.getOriginal_pic();
        } else {
            thumbnail_pic = "";
            bmiddle_pic = "";
            original_pic = "";
        }
        user = new User(json.getJSONObject("user"));
    }

    private void init(Response res, Element elem, Weibo weibo) throws
            HttpException {
        ensureRootNodeNameIs("status", elem);
        user = new User(res, (Element) elem.getElementsByTagName("user").item(0)
                , weibo);
        id = getChildString("id", elem);
        text = getChildText("text", elem);
        source = getChildText("source", elem);
        createdAt = getChildDate("created_at", elem);
        isTruncated = getChildBoolean("truncated", elem);
        inReplyToStatusId = getChildString("in_reply_to_status_id", elem);
        inReplyToUserId = getChildString("in_reply_to_user_id", elem);
        isFavorited = getChildBoolean("favorited", elem);
        inReplyToScreenName = getChildText("in_reply_to_screen_name", elem);
        NodeList georssPoint = elem.getElementsByTagName("georss:point");
        
        if(1 == georssPoint.getLength()){
            String[] point = georssPoint.item(0).getFirstChild().getNodeValue().split(" ");
            if(!"null".equals(point[0]))
                latitude = Double.parseDouble(point[0]);
            if(!"null".equals(point[1]))
                longitude = Double.parseDouble(point[1]);
        }
        NodeList retweetDetailsNode = elem.getElementsByTagName("retweet_details");
        if(1 == retweetDetailsNode.getLength()){
            retweetDetails = new RetweetDetails(res,(Element)retweetDetailsNode.item(0),weibo);
        }
    }

    /**
     * Return the created_at
     *
     * @return created_at
     * @since Weibo4J 1.1.0
     */

    public Date getCreatedAt() {
        return this.createdAt;
    }

    /**
     * Returns the id of the status
     *
     * @return the id
     */
    public String getId() {
        return this.id;
    }

    /**
     * Returns the text of the status
     *
     * @return the text
     */
    public String getText() {
        return this.text;
    }

    /**
     * Returns the source
     *
     * @return the source
     * @since Weibo4J 1.0.4
     */
    public String getSource() {
        return this.source;
    }


    /**
     * Test if the status is truncated
     *
     * @return true if truncated
     * @since Weibo4J 1.0.4
     */
    public boolean isTruncated() {
        return isTruncated;
    }

    /**
     * Returns the in_reply_tostatus_id
     *
     * @return the in_reply_tostatus_id
     * @since Weibo4J 1.0.4
     */
    public String getInReplyToStatusId() {
        return inReplyToStatusId;
    }

    /**
     * Returns the in_reply_user_id
     *
     * @return the in_reply_tostatus_id
     * @since Weibo4J 1.0.4
     */
    public String getInReplyToUserId() {
        return inReplyToUserId;
    }

    /**
     * Returns the in_reply_to_screen_name
     *
     * @return the in_in_reply_to_screen_name
     * @since Weibo4J 2.0.4
     */
    public String getInReplyToScreenName() {
        return inReplyToScreenName;
    }

    /**
     * returns The location's latitude that this tweet refers to.
     *
     * @since Weibo4J 2.0.10
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * returns The location's longitude that this tweet refers to.
     *
     * @since Weibo4J 2.0.10
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Test if the status is favorited
     *
     * @return true if favorited
     * @since Weibo4J 1.0.4
     */
    public boolean isFavorited() {
        return isFavorited;
    }

    public String getThumbnail_pic() {
        return thumbnail_pic;
    }

    public String getBmiddle_pic() {
        return bmiddle_pic;
    }

    public String getOriginal_pic() {
        return original_pic;
    }


    /**
     * Return the user
     *
     * @return the user
     */
    public User getUser() {
        return user;
    }
    
    // TODO: 等合并Tweet, FanStatus
    public int getType() {
        return -1111111;
    }

    /**
     *
     * @since Weibo4J 2.0.10
     */
    public boolean isRetweet(){
        return null != retweetDetails;
    }

    /**
     *
     * @since Weibo4J 2.0.10
     */
    public RetweetDetails getRetweetDetails() {
        return retweetDetails;
    }
    
    public void setStatusType(int type) {
        statusType = type;
    }
    public int getStatusType() {
        return statusType;
    }


    /*package*/
    static List<FanStatus> constructStatuses(Response res,
                                          Weibo weibo) throws HttpException {
        
         Document doc = res.asDocument();
        if (isRootNodeNilClasses(doc)) {
            return new ArrayList<FanStatus>(0);
        } else {
            try {
                ensureRootNodeNameIs("statuses", doc);
                NodeList list = doc.getDocumentElement().getElementsByTagName(
                        "status");
                int size = list.getLength();
                List<FanStatus> statuses = new ArrayList<FanStatus>(size);
                for (int i = 0; i < size; i++) {
                    Element status = (Element) list.item(i);
                    statuses.add(new FanStatus(res, status, weibo));
                }
                return statuses;
            } catch (HttpException te) {
                ensureRootNodeNameIs("nil-classes", doc);
                return new ArrayList<FanStatus>(0);
            }
        }
       
    }

    /*modify by sycheng add json call method*/
    /*package*/
    static List<FanStatus> constructStatuses(Response res) throws HttpException {
         try {
             JSONArray list = res.asJSONArray();
             int size = list.length();
             List<FanStatus> statuses = new ArrayList<FanStatus>(size);
             for (int i = 0; i < size; i++) {
                 statuses.add(new FanStatus(list.getJSONObject(i)));
             }
             return statuses;
         } catch (JSONException jsone) {
             throw new HttpException(jsone);
         } catch (HttpException te) {
             throw te;
         }  
       
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public boolean equals(Object obj) {
        if (null == obj) {
            return false;
        }
        if (this == obj) {
            return true;
        }
//      return obj instanceof FanStatus && ((FanStatus) obj).id == this.id;
        return obj instanceof FanStatus && this.id.equals(((FanStatus) obj).id);
    }

    @Override
    public String toString() {
        return "FanStatus{" +
                "createdAt=" + createdAt +
                ", id=" + id +
                ", text='" + text + '\'' +
                ", source='" + source + '\'' +
                ", isTruncated=" + isTruncated +
                ", inReplyToStatusId=" + inReplyToStatusId +
                ", inReplyToUserId=" + inReplyToUserId +
                ", isFavorited=" + isFavorited +
                ", thumbnail_pic=" + thumbnail_pic +
                ", bmiddle_pic=" + bmiddle_pic +
                ", original_pic=" + original_pic +
                ", inReplyToScreenName='" + inReplyToScreenName + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", retweetDetails=" + retweetDetails +
                ", user=" + user +
                '}';
    }
    
    public boolean isEmpty() {
        return (null == id);
    }
    
    // Old Tweet
    
    public static String buildMetaText(StringBuilder builder,
            Date createdAt, String source, String replyTo) {
          builder.setLength(0);

          builder.append(Utils.getRelativeDate(createdAt));
          builder.append(" ");
          
          builder.append(TwitterApplication.mContext.getString(R.string.tweet_source_prefix));
          builder.append(source);
          
          if (!Utils.isEmpty(replyTo)) {
              builder.append(" " + TwitterApplication.mContext.getString(R.string.tweet_reply_to_prefix));
              builder.append(replyTo);
              builder.append(TwitterApplication.mContext.getString(R.string.tweet_reply_to_suffix));
          }

          return builder.toString();
        }
    
    // Database 
    
    /**
     * 向Status表中写入一行数据, 此方法为私有方法, 外部插入数据请使用 putTweets()
     * 
     * @param tweet
     *            需要写入的单条消息
     * @return the row ID of the newly inserted row, or -1 if an error occurred
     */
    @Override
    public long insert() {
        SQLiteDatabase Db = TwitterDatabase.getDb(true);
        ContentValues values = this.toContentValues();

        if (isExists(values.getAsString("id"),
                     values.getAsString("user_id"),
                     values.getAsInteger("type")))
        {
            Log.i(TAG, values.get("id") + "is exists.");
            return -1;
        }

        long id = Db.insert(StatusTable.TABLE_NAME, null, values);
        
        if (-1 == id) {
            Log.e(TAG, "cann't insert the status : " + values.toString());
        } else {
            Log.i(TAG, "Insert a status into datebase : " + values.toString());
        }

        return id;
    }

    /**
     * 删除一条消息
     * 
     * @param tweetId
     * @param type -1 means all types
     * @return the number of rows affected if a whereClause is passed in, 0
     *         otherwise. To remove all rows and get a count pass "1" as the
     *         whereClause.
     */
    @Override
    public int delete() {
        SQLiteDatabase db = TwitterDatabase.getDb(true);
        
        String where = StatusTable._ID + " =? "
                     + " AND " + StatusTable.FIELD_OWNER_ID + " = '" + user.getId() + "' ";
        if (-1 != statusType) {
           where  += " AND " + StatusTable.FIELD_STATUS_TYPE + " = " + statusType;
        }

        return db.delete(StatusTable.TABLE_NAME, where, new String[] { id });
    }


    @Override
    /**
     * 更新一条消息
     * 
     * @param tweetId
     * @param values
     *            ContentValues 需要更新字段的键值对
     * @return the number of rows affected, return -1 when no need to update
     */
    public int update() {
        if (isDirty) {
            Log.i(TAG, "Update Tweet  : " + id + " " + toString());
            SQLiteDatabase Db = TwitterDatabase.getDb(true);
            return Db.update(StatusTable.TABLE_NAME, toContentValues(), // new Values
                    StatusTable._ID + "=?", new String[] { id });
        }
        return -1;
    }

    @Override
    public Cursor select() {
        // TODO Auto-generated method stub
        return null;
    }
    
    /**
     * 快速检查某条消息是否存在(指定类型)
     * 
     * @param tweetId
     * @param type
     *            <li>StatusTable.TYPE_HOME</li>
     *            <li>StatusTable.TYPE_MENTION</li>
     *            <li>StatusTable.TYPE_USER</li>
     *            <li>StatusTable.TYPE_FAVORITE</li>
     * @return is exists
     */
    public boolean isExists(String tweetId, String owner, int type) {
        SQLiteDatabase Db = TwitterDatabase.getDb(true);
        boolean result = false;

        Cursor cursor = Db.query(StatusTable.TABLE_NAME,
                new String[] { StatusTable._ID }, StatusTable._ID + " =? AND "
                    + StatusTable.FIELD_OWNER_ID + "=? AND "    
                    + StatusTable.FIELD_STATUS_TYPE + " = " + type,
                new String[] { tweetId, owner }, null, null, null);

        if (cursor != null && cursor.getCount() > 0) {
            result = true;
        }

        cursor.close();
        return result;
    }
    
    // TODO: 考虑是否可以通过使用转换为 ContentValues 来替代Parcelable
    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        return values;
    }
}