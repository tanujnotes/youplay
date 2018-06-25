package com.youplay;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by tan on 27/02/17.
 **/

public class PlaylistItemsDeserializer implements JsonDeserializer<ItemModel> {
    @Override
    public ItemModel deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Gson gson = new Gson();

        try {
            ItemModel itemModel = gson.fromJson(json, ItemModel.class);
            List<ItemModel.Item> itemList = itemModel.getItems();
            int size = itemList.size();
            for (int i = 0; i < size; i++) {
                itemList.get(i).getSnippet().setResourceId(itemList.get(i).getId());
            }
            itemModel.setItems(itemList);
            return itemModel;

        } catch (Exception e) {
            PlaylistItemsModel playlistItemsModel = gson.fromJson(json, PlaylistItemsModel.class);

            ItemModel itemModel = new ItemModel();
            itemModel.setEtag(playlistItemsModel.getEtag());
            itemModel.setKind(playlistItemsModel.getKind());
            itemModel.setPageInfo(playlistItemsModel.getPageInfo());
            itemModel.setNextPageToken(playlistItemsModel.getNextPageToken());

            List<ItemModel.Item> itemList = new ArrayList<>();
            List<PlaylistItemsModel.Item> playlistItemsList = playlistItemsModel.getItems();
            int size = playlistItemsList.size();
            for (int i = 0; i < size; i++) {
                ItemModel.Item item = new ItemModel.Item();
                item.setSnippet(playlistItemsList.get(i).getSnippet());
                item.setContentDetails(playlistItemsList.get(i).getContentDetails());
                item.setStatistics(playlistItemsList.get(i).getStatistics());
                item.getSnippet().setPlaylistId(playlistItemsList.get(i).getId());
                itemList.add(item);
            }
            itemModel.setItems(itemList);
            return itemModel;
        }
    }
}
