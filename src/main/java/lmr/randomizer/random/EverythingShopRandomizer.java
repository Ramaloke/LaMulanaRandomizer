package lmr.randomizer.random;

import lmr.randomizer.DataFromFile;
import lmr.randomizer.FileUtils;
import lmr.randomizer.Settings;
import lmr.randomizer.Translations;
import lmr.randomizer.dat.Block;
import lmr.randomizer.dat.shop.ShopBlock;
import lmr.randomizer.node.AccessChecker;
import lmr.randomizer.update.GameDataTracker;
import lmr.randomizer.update.GameObjectId;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

/**
 * Created by thezerothcat on 8/2/2017.
 *
 * Currently only randomizes unique items. Should probably eventually randomize ammo options also.
 */
public class EverythingShopRandomizer implements ShopRandomizer {
    private static final String MSX_SHOP_NAME = "Shop 2 Alt (Surface)";
    private static final String NON_MSX_SHOP_NAME = "Shop 2 (Surface)";
    private static final String FISH_SHOP_NAME = "Shop 12 (Spring)";
    private static final String FISH_FAIRY_SHOP_NAME = "Shop 12 Alt (Spring)";
    private static final String LITTLE_BROTHER_SHOP_NAME = "Shop 18 (Lil Bro)";

    private AccessChecker accessChecker;
    private ItemRandomizer itemRandomizer; // Not needed for this version of the randomizer?

    private Map<String, String> mapOfShopInventoryItemToContents = new HashMap<>(); // The thing we're trying to build.
    private List<String> unassignedShopItemLocations = new ArrayList<>(); // Shop locations which still need something placed.
    private List<String> randomizedShops;

    private List<String> shopsWithSacredOrbs;

    public EverythingShopRandomizer() {
        randomizedShops = new ArrayList<>(DataFromFile.getAllShops());

        for(String shop : DataFromFile.getAllShops()) {
            if(MSX_SHOP_NAME.equals(shop)) {
                unassignedShopItemLocations.add(String.format("%s Item 1", shop));
            }
            else {
                for (int i = 0; i < 3; i++) {
                    unassignedShopItemLocations.add(String.format("%s Item %d", shop, i + 1));
                }
            }
        }

        shopsWithSacredOrbs = new ArrayList<>();
    }

    public List<String> getUnassignedShopItemLocations() {
        return unassignedShopItemLocations;
    }

    public void placeNonRandomizedItems() {
        List<String> originalShopContents;
        String originalShopItem;
        String shopItemLocation;

        List<String> nonRandomizedItems = DataFromFile.getNonRandomizedItems();
        for(String shop : randomizedShops) {
            originalShopContents = DataFromFile.getMapOfShopNameToShopOriginalContents().get(shop);
            for (int i = 0; i < 3; i++) {
                shopItemLocation = String.format("%s Item %d", shop, i + 1);
                originalShopItem = originalShopContents.get(i);
                if(originalShopItem.equals("Weights") || originalShopItem.endsWith("Ammo")) {
                    continue;
                }
                else if(Settings.getStartingItems().contains(originalShopItem)) {
                    continue;
                }
                else if(MSX_SHOP_NAME.equals(shop) && "Mobile Super X2".equals(originalShopItem) && nonRandomizedItems.contains(originalShopItem)) {
                    mapOfShopInventoryItemToContents.put(shopItemLocation, originalShopItem);
                    unassignedShopItemLocations.remove(shopItemLocation);
                    itemRandomizer.removeItemFromUnplacedItems(originalShopItem);
                }
                else if(FISH_FAIRY_SHOP_NAME.equals(shop) && !"Shell Horn".equals(originalShopItem) && !"guild.exe".equals(originalShopItem) && nonRandomizedItems.contains(originalShopItem)) {
                    mapOfShopInventoryItemToContents.put(shopItemLocation, originalShopItem);
                    unassignedShopItemLocations.remove(shopItemLocation);
                    itemRandomizer.removeItemFromUnplacedItems(originalShopItem);
                }
                else if(nonRandomizedItems.contains(originalShopItem)){
                    mapOfShopInventoryItemToContents.put(shopItemLocation, originalShopItem);
                    unassignedShopItemLocations.remove(shopItemLocation);
                    itemRandomizer.removeItemFromUnplacedItems(originalShopItem);
                }
            }
        }
//        if(Settings.isRandomizeMainWeapon() && !"Whip".equals(Settings.getCurrentStartingWeapon())) {
//            mapOfShopInventoryItemToContents.put("Shop 3 (Surface) Item 1", "Whip");
//            unassignedShopItemLocations.remove("Shop 3 (Surface) Item 1");
//            itemRandomizer.removeItemFromUnplacedItems("Whip");
//        }
//        mapOfShopInventoryItemToContents.put("Shop 3 (Surface) Item 1", "Sacred Orb (Gate of Guidance)");
//        unassignedShopItemLocations.remove("Shop 3 (Surface) Item 1");
//        itemRandomizer.removeItemFromUnplacedItems("Sacred Orb (Gate of Guidance)");
//        mapOfShopInventoryItemToContents.put("Shop 2 Alt (Surface) Item 1", "Diary");
//        unassignedShopItemLocations.remove("Shop 2 Alt (Surface) Item 1");
//        itemRandomizer.removeItemFromUnplacedItems("Diary");
//        mapOfShopInventoryItemToContents.put("Shop 18 (Lil Bro) Item 1", "Fruit of Eden");
//        unassignedShopItemLocations.remove("Shop 18 (Lil Bro) Item 1");
//        itemRandomizer.removeItemFromUnplacedItems("Fruit of Eden");
    }

    public List<String> getShopItems(String shopName) {
        List<String> shopItems = new ArrayList<>();
        String shopItem;
        for (int i = 1; i <= 3; i++) {
            shopItem = mapOfShopInventoryItemToContents.get(String.format("%s Item %d", shopName, i));
            if (shopItem != null && !"Weights".equals(shopItem)) {
                shopItems.add(shopItem);
            }
        }
        return shopItems;
    }

    public boolean placeRequiredItem(String item, List<String> shopLocationOptions, int locationIndex) {
        String location = shopLocationOptions.get(locationIndex);
        if(accessChecker.validRequirements(item, location)) {
            mapOfShopInventoryItemToContents.put(location, item);
            shopLocationOptions.remove(location);
            unassignedShopItemLocations.remove(location);
            return true;
        }
        return false;
    }

    public boolean placeItem(String item, int locationIndex) {
        String location = unassignedShopItemLocations.get(locationIndex);
        if(accessChecker.validRequirements(item, location)) {
            mapOfShopInventoryItemToContents.put(location, item);
            unassignedShopItemLocations.remove(location);
            if(item.contains("Sacred Orb")) {
                shopsWithSacredOrbs.add(location.substring(0, location.indexOf(")") + 1));
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean shopContainsSacredOrb(String shopName) {
        return shopsWithSacredOrbs.contains(shopName);
    }

    public List<String> getInitialUnassignedShopItemLocations() {
        List<String> initialUnassigned = new ArrayList<>();
        List<String> initialShops = DataFromFile.getInitialShops();
        for(String shopName : initialShops) {
            for(String unassigned : unassignedShopItemLocations) {
                if(unassigned.contains(shopName)) {
                    initialUnassigned.add(unassigned);
                }
            }
        }
        return initialUnassigned;
    }

    public List<String> getPlacedShopItems() {
        List<String> placedItems = new ArrayList<>();
        for(Map.Entry<String, String> locationAndItem : mapOfShopInventoryItemToContents.entrySet()) {
            placedItems.add(locationAndItem.getValue());
        }
        return placedItems;
    }

    public void determineItemTypes(Random random) {
        assignWeights(random);
        assignSubweaponAmmoLocations(random);
    }

    private void assignWeights(Random random) {
        List<String> shopsWithNoWeights = new ArrayList<>(randomizedShops);
        shopsWithNoWeights.remove(LITTLE_BROTHER_SHOP_NAME);
        for(Map.Entry<String, String> shopLocationAndItem : mapOfShopInventoryItemToContents.entrySet()) {
            if(shopLocationAndItem.getKey().contains("Surface") && shopLocationAndItem.getValue().equals("Weights")) {
                String surfaceWeightsLocation = shopLocationAndItem.getKey();
                String surfaceWeightsShop = surfaceWeightsLocation.substring(0, surfaceWeightsLocation.indexOf(")") + 1);
                shopsWithNoWeights.remove(surfaceWeightsShop);
                if(MSX_SHOP_NAME.equals(surfaceWeightsShop)) {
                    shopsWithNoWeights.remove(NON_MSX_SHOP_NAME);
                }
                else if(NON_MSX_SHOP_NAME.equals(surfaceWeightsShop)) {
                    shopsWithNoWeights.remove(MSX_SHOP_NAME);
                }
                break;
            }
        }

        String shop;
        String location;
        List<Integer> shopItemNumbers;
        int shopItemNumberIndex;
        int maxAdditionalWeights = getWeightCount(random);
        int weightsPlaced = 0;
        while(weightsPlaced < maxAdditionalWeights) {
            location = null;
            shop = shopsWithNoWeights.get(random.nextInt(shopsWithNoWeights.size()));
            shopItemNumbers = new ArrayList<>(Arrays.asList(1, 2, 3));
            while(!shopItemNumbers.isEmpty()) {
                shopItemNumberIndex = random.nextInt(shopItemNumbers.size());
                location = String.format("%s Item %d", shop, shopItemNumbers.get(shopItemNumberIndex));
                if(location.equals(MSX_SHOP_NAME + " Item 1")) {
                    // Don't put weights/ammo where MSX2 was.
                    shopItemNumbers.remove(shopItemNumberIndex);
                    continue;
                }

                if(unassignedShopItemLocations.contains(location)) {
                    mapOfShopInventoryItemToContents.put(location, "Weights");
                    unassignedShopItemLocations.remove(location);
                    ++weightsPlaced;
                    break;
                }
                shopItemNumbers.remove(shopItemNumberIndex);
            }
            if(location != null) {
                shopsWithNoWeights.remove(shop);
                if(MSX_SHOP_NAME.equals(shop) && (location.contains("Item 2") || location.contains("Item 3"))) {
                    shopsWithNoWeights.remove(NON_MSX_SHOP_NAME);
                }
                else if(NON_MSX_SHOP_NAME.equals(shop) && (location.contains("Item 2") || location.contains("Item 3"))) {
                    shopsWithNoWeights.remove(MSX_SHOP_NAME);
                }
            }
        }
    }

    public String placeGuaranteedWeights(Random random) {
        // Guarantee weight shop on the Surface
        List<String> guaranteedWeightShopLocations = new ArrayList<>();
        for(String location : unassignedShopItemLocations) {
            if(location.contains("Surface") && !location.equals(MSX_SHOP_NAME + " Item 1")) {
                guaranteedWeightShopLocations.add(location);
            }
        }
        String surfaceWeightsLocation = guaranteedWeightShopLocations.get(random.nextInt(guaranteedWeightShopLocations.size()));
        mapOfShopInventoryItemToContents.put(surfaceWeightsLocation, "Weights");
        unassignedShopItemLocations.remove(surfaceWeightsLocation);

        // Guarantee weights at Little Brother's shop so there's a guaranteed way to unlock Big Brother's shop.
        guaranteedWeightShopLocations.clear();
        for(String location : unassignedShopItemLocations) {
            if(location.contains(LITTLE_BROTHER_SHOP_NAME)) {
                guaranteedWeightShopLocations.add(location);
            }
        }
        String littleBrotherShopWeightsLocation = guaranteedWeightShopLocations.get(random.nextInt(guaranteedWeightShopLocations.size()));
        mapOfShopInventoryItemToContents.put(littleBrotherShopWeightsLocation, "Weights");
        unassignedShopItemLocations.remove(littleBrotherShopWeightsLocation);
        return surfaceWeightsLocation;
    }

    private int getWeightCount(Random random) {
        int maxAdditionalWeights = Math.min(randomizedShops.size() - 4,
                unassignedShopItemLocations.size() - ItemRandomizer.ALL_SUBWEAPONS.size() - itemRandomizer.getTotalShopItems()); // Must have enough room for all shop items plus one of each ammo type plus one weight. The remaining ammo to weights ratio can be random.
        if(maxAdditionalWeights < 0) {
            maxAdditionalWeights = 0;
        }
        return random.nextInt(maxAdditionalWeights);
    }

    private void assignSubweaponAmmoLocations(Random random) {
        List<String> unassignedSubweapons = new ArrayList<>(ItemRandomizer.ALL_SUBWEAPONS);
        int totalSubweaponLocations = unassignedShopItemLocations.size() - itemRandomizer.getTotalShopItems();

        String location;
        String subweapon;
        int shopLocationIndex;
        for(int i = 0; i < totalSubweaponLocations; i++) {
            shopLocationIndex = random.nextInt(unassignedShopItemLocations.size());
            location = unassignedShopItemLocations.get(shopLocationIndex);
            while(location.equals(MSX_SHOP_NAME + " Item 1")) {
                shopLocationIndex = random.nextInt(unassignedShopItemLocations.size());
                location = unassignedShopItemLocations.get(shopLocationIndex);
            }

            if(unassignedSubweapons.isEmpty()) {
                subweapon = ItemRandomizer.ALL_SUBWEAPONS.get(random.nextInt(ItemRandomizer.ALL_SUBWEAPONS.size()));
            }
            else {
                int unassignedSubweaponIndex = random.nextInt(unassignedSubweapons.size());
                subweapon = unassignedSubweapons.get(unassignedSubweaponIndex);
                if(!location.startsWith(FISH_SHOP_NAME)) {
                    unassignedSubweapons.remove(unassignedSubweaponIndex);
                }
            }
            mapOfShopInventoryItemToContents.put(location, subweapon + " Ammo");
            unassignedShopItemLocations.remove(location);
        }
    }

    @Override
    public String findNameOfShopNodeContainingItem(String itemToLookFor) {
        for(Map.Entry<String, String> shopNameAndContents : mapOfShopInventoryItemToContents.entrySet()) {
            if(shopNameAndContents.getValue().equals(itemToLookFor)) {
                return shopNameAndContents.getKey().substring(0, shopNameAndContents.getKey().indexOf(")") + 1);
            }
        }
        return null;
    }

    public void outputLocations(int attemptNumber) throws IOException {
        BufferedWriter writer = FileUtils.getFileWriter(String.format("%d/shops.txt", Settings.getStartingSeed()));
        if (writer == null) {
            return;
        }


        String location;
        for(String shop : randomizedShops) {
            for (int i = 1; i <= 3; i++) {
                location = String.format("%s Item %d", shop, i);
                if(mapOfShopInventoryItemToContents.containsKey(location)) {
                    String itemName = Settings.getUpdatedContents(mapOfShopInventoryItemToContents.get(location));
                    boolean removedItem = Settings.getCurrentRemovedItems().contains(itemName)
                            || Settings.getRemovedItems().contains(itemName)
                            || Settings.getStartingItems().contains(itemName)
                            || (Settings.isReplaceMapsWithWeights() && itemName.startsWith("Map (") && !"Map (Shrine of the Mother)".equals(itemName));
                    writer.write(Translations.getShopItemText(shop, i) + ": " + Translations.getItemText(itemName, removedItem));
                    writer.newLine();
                }
                else {
                    writer.write(Translations.getShopItemText(shop, i) + ": (unchanged)");
                    writer.newLine();
                }
            }
        }

        writer.flush();
        writer.close();
    }

    public void updateFiles(List<Block> blocks, Random random) {
        String shopItem1;
        String shopItem2;
        String shopItem3;
        ShopBlock shopBlock;
        for(String shopName : randomizedShops) {
            shopBlock = (ShopBlock) blocks.get(DataFromFile.getMapOfShopNameToShopBlock().get(shopName));

            if(MSX_SHOP_NAME.equals(shopName)) {
                shopItem1 = Settings.getUpdatedContents(mapOfShopInventoryItemToContents.get(String.format("%s Item 1", shopName)));
                shopItem2 = Settings.getUpdatedContents(mapOfShopInventoryItemToContents.get(String.format("%s Item 2", NON_MSX_SHOP_NAME)));
                shopItem3 = Settings.getUpdatedContents(mapOfShopInventoryItemToContents.get(String.format("%s Item 3", NON_MSX_SHOP_NAME)));

                // No need to worry about flag replacement because MSX2 can't be a removed item.
                Map<String, GameObjectId> nameToDataMap = DataFromFile.getMapOfItemToUsefulIdentifyingRcdData();
                GameObjectId itemNewContentsData = nameToDataMap.get(shopItem1);
                GameDataTracker.writeLocationContents("Mobile Super X2", shopItem1,
                        nameToDataMap.get("Mobile Super X2"), itemNewContentsData, itemNewContentsData.getWorldFlag(), random);
            }
            else {
                shopItem1 = Settings.getUpdatedContents(mapOfShopInventoryItemToContents.get(String.format("%s Item 1", shopName)));
                shopItem2 = Settings.getUpdatedContents(mapOfShopInventoryItemToContents.get(String.format("%s Item 2", shopName)));
                shopItem3 = Settings.getUpdatedContents(mapOfShopInventoryItemToContents.get(String.format("%s Item 3", shopName)));
            }

            if(Settings.getCurrentRemovedItems().contains(shopItem1)
                    || Settings.getRemovedItems().contains(shopItem1)
                    || Settings.getStartingItems().contains(shopItem1)
                    || (Settings.isReplaceMapsWithWeights() && shopItem1.startsWith("Map (") && !"Map (Shrine of the Mother)".equals(shopItem1))) {
                shopItem1 = "Weights";
            }
            if(Settings.getCurrentRemovedItems().contains(shopItem2)
                    || Settings.getRemovedItems().contains(shopItem2)
                    || Settings.getStartingItems().contains(shopItem2)
                    || (Settings.isReplaceMapsWithWeights() && shopItem2.startsWith("Map (") && !"Map (Shrine of the Mother)".equals(shopItem2))) {
                shopItem2 = "Weights";
            }
            if(Settings.getCurrentRemovedItems().contains(shopItem3)
                    || Settings.getRemovedItems().contains(shopItem3)
                    || Settings.getStartingItems().contains(shopItem3)
                    || (Settings.isReplaceMapsWithWeights() && shopItem3.startsWith("Map (") && !"Map (Shrine of the Mother)".equals(shopItem3))) {
                shopItem3 = "Weights";
            }
            GameDataTracker.writeShopInventory(shopBlock, shopItem1, shopItem2, shopItem3, blocks, new ShopItemPriceCountRandomizer(random),
                    "Shop 18 (Lil Bro)".equals(shopName), MSX_SHOP_NAME.equals(shopName));
        }
    }

    public void setAccessChecker(AccessChecker accessChecker) {
        this.accessChecker = accessChecker;
    }

    public void setItemRandomizer(ItemRandomizer itemRandomizer) {
        this.itemRandomizer = itemRandomizer;
    }
}
