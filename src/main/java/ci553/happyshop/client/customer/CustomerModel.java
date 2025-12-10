package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Order;
import ci553.happyshop.catalogue.Product;
import ci553.happyshop.storageAccess.DatabaseRW;
import ci553.happyshop.orderManagement.OrderHub;
import ci553.happyshop.utility.StorageLocation;
import ci553.happyshop.utility.ProductListFormatter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO
 * You can either directly modify the CustomerModel class to implement the required tasks,
 * or create a subclass of CustomerModel and override specific methods where appropriate.
 */
public class CustomerModel {
    public CustomerView cusView;
    public DatabaseRW databaseRW; //Interface type, not specific implementation
                                  //Benefits: Flexibility: Easily change the database implementation.

    private Product theProduct =null; // product found from search
    private ArrayList<Product> trolley =  new ArrayList<>(); // a list of products in trolley

    // Five UI elements to be passed to CustomerView for display updates.
    private String imageName = "imageHolder.jpg";                // Image to show in product preview (Search Page)
    private String displayLaSearchResult = "No Product was searched yet"; // Label showing search result message (Search Page)
    private String displayTaTrolley = "";                                // Text area content showing current trolley items (Trolley Page)
    private String displayTaReceipt = "";                                // Text area content showing receipt after checkout (Receipt Page)
    public RemoveProductNotifier removeProductNotifier;                  // Updated notifier when stock is shortage

    //SELECT productID, description, image, unitPrice,inStock quantity
    void search() throws SQLException {
        // User can type either a Product ID or a Name into the ID field
        String input = cusView.tfId.getText().trim();
        searchWithInput(input);
    }
    // Helper used by both GUI and unit tests
    void searchWithInput(String input) throws SQLException {
        if (input == null) {
            input = "";
        }

        if (input.isEmpty()) {
            theProduct = null;
            displayLaSearchResult = "Please type ProductID or Name";
            System.out.println("Please type ProductID or Name.");

            // clear search results list
            cusView.updateSearchResults(new ArrayList<>());
            updateView();
            return;
        }

        // 1) Try search by Product ID first (original behaviour)
        Product foundById = databaseRW.searchByProductId(input);

        if (foundById != null && foundById.getStockQuantity() > 0) {
            theProduct = foundById;

            double unitPrice = theProduct.getUnitPrice();
            String description = theProduct.getProductDescription();
            int stock = theProduct.getStockQuantity();

            String baseInfo = String.format(
                    "Product_Id: %s\n%s,\nPrice: £%.2f",
                    theProduct.getProductId(),
                    description,
                    unitPrice
            );
            String quantityInfo = stock < 100 ? String.format("\n%d units left.", stock) : "";
            displayLaSearchResult = baseInfo + quantityInfo;

            // list contains just this product
            ArrayList<Product> singleList = new ArrayList<>();
            singleList.add(foundById);
            cusView.updateSearchResults(singleList);

            System.out.println(displayLaSearchResult);
            updateView();
            return;
        }

        // 2) If not found by ID (or out of stock), try search by Name/Keyword
        ArrayList<Product> resultList = databaseRW.searchProduct(input);

        if (resultList != null && !resultList.isEmpty()) {
            // Use the first product as the selected one
            theProduct = resultList.get(0);

            double unitPrice = theProduct.getUnitPrice();
            String description = theProduct.getProductDescription();
            int stock = theProduct.getStockQuantity();

            String baseInfo = String.format(
                    "Product_Id: %s\n%s,\nPrice: £%.2f",
                    theProduct.getProductId(),
                    description,
                    unitPrice
            );
            String quantityInfo = stock < 100 ? String.format("\n%d units left.", stock) : "";
            displayLaSearchResult = baseInfo + quantityInfo;

            // Update the list like WarehouseView
            cusView.updateSearchResults(resultList);

            System.out.println(displayLaSearchResult);
        } else {
            theProduct = null;
            displayLaSearchResult = "No Product was found with ID or Name \"" + input + "\"";
            System.out.println(displayLaSearchResult);

            cusView.updateSearchResults(new ArrayList<>());
        }

        updateView();
    }


    void addToTrolley(){
        if(theProduct!= null){

            // trolley.add(theProduct) — Product is appended to the end of the trolley.
            // To keep the trolley organized, add code here or call a method that:
            //TODO
            // 1. Merges items with the same product ID (combining their quantities).
            // 2. Sorts the products in the trolley by product ID.
            //trolley.add(theProduct);
            makeOrganizedTrolley();
            displayTaTrolley = ProductListFormatter.buildString(trolley); //build a String for trolley so that we can show it
        }
        else{
            displayLaSearchResult = "Please search for an available product before adding it to the trolley";
            System.out.println("must search and get an available product before add to trolley");
        }
        displayTaReceipt=""; // Clear receipt to switch back to trolleyPage (receipt shows only when not empty)
        updateView();
    }

    void makeOrganizedTrolley() {
        for (Product p : trolley) {
            if (p.getProductId().equals(theProduct.getProductId())) {
                // Each time the same product is added, increase ordered quantity by 1
                p.setOrderedQuantity(p.getOrderedQuantity() + 1);
                // start sorting the products according to their Product id
                trolley.sort((p1, p2) -> p1.getProductId().compareTo(p2.getProductId()));
                return;
            }
        }

        // First time a product goes into the trolley
        Product pNew = new Product(
                theProduct.getProductId(),
                theProduct.getProductDescription(),
                theProduct.getProductImageName(),
                theProduct.getUnitPrice(),
                theProduct.getStockQuantity()
        );
        pNew.setOrderedQuantity(1); // start counting the product quantity from 1
        trolley.add(pNew);

        // Keep trolley sorted by product ID
        trolley.sort((p1, p2) -> p1.getProductId().compareTo(p2.getProductId()));
    }


    void checkOut() throws IOException, SQLException {
        if(!trolley.isEmpty()){
            // Group the products in the trolley by productId to optimize stock checking
            // Check the database for sufficient stock for all products in the trolley.
            // If any products are insufficient, the update will be rolled back.
            // If all products are sufficient, the database will be updated, and insufficientProducts will be empty.
            // Note: If the trolley is already organized (merged and sorted), grouping is unnecessary.

            //ArrayList<Product> groupedTrolley= groupProductsById(trolley);
            // Trolley is already organized (one product per ID with correct orderedQuantity)
            ArrayList<Product> insufficientProducts = databaseRW.purchaseStocks(trolley);


            if(insufficientProducts.isEmpty()){ // If stock is sufficient for all products
                //get OrderHub and tell it to make a new Order
                OrderHub orderHub =OrderHub.getOrderHub();
                Order theOrder = orderHub.newOrder(trolley);
                trolley.clear();
                displayTaTrolley ="";

                //if a previous error window is still open, it closes when checkout finally succeeds.
                if (removeProductNotifier != null) {
                    removeProductNotifier.closeNotifierWindow();
                }
                displayTaReceipt = String.format(
                        "Order_ID: %s\nOrdered_Date_Time: %s\n%s",
                        theOrder.getOrderId(),
                        theOrder.getOrderedDateTime(),
                        ProductListFormatter.buildString(theOrder.getProductList())
                );
                System.out.println(displayTaReceipt);
            }
            else{ // Some products have insufficient stock — build an error message to inform the customer
                StringBuilder errorMsg = new StringBuilder();
                for(Product p : insufficientProducts){
                    errorMsg.append("\u2022 "+ p.getProductId()).append(", ")
                            .append(p.getProductDescription()).append(" (Only ")
                            .append(p.getStockQuantity()).append(" available, ")
                            .append(p.getOrderedQuantity()).append(" requested)\n");
                }
                theProduct=null;

                // 1. Remove products with insufficient stock from the trolley
                for (Product insufficient : insufficientProducts) {
                    trolley.removeIf(p -> p.getProductId().equals(insufficient.getProductId()));
                }

                // Update trolley text after removal
                displayTaTrolley = ProductListFormatter.buildString(trolley);

                // 2. Show message window instead of just changing the label
                String fullMsg =
                        "Checkout failed due to insufficient stock for the following products:\n"
                                + errorMsg.toString();

                if (removeProductNotifier != null) {
                    removeProductNotifier.showRemovalMsg(fullMsg);
                } else {
                    // Fallback if notifier is not wired (e.g. in testing)
                    displayLaSearchResult = fullMsg;
                }

                System.out.println("stock is not enough");
            }
        }
        else{
            displayTaTrolley = "Your trolley is empty";
            System.out.println("Your trolley is empty");
        }
        updateView();
    }

    /**
     * Groups products by their productId to optimize database queries and updates.
     * By grouping products, we can check the stock for a given `productId` once, rather than repeatedly
     */
    private ArrayList<Product> groupProductsById(ArrayList<Product> proList) {
        Map<String, Product> grouped = new HashMap<>();
        for (Product p : proList) {
            String id = p.getProductId();
            if (grouped.containsKey(id)) {
                Product existing = grouped.get(id);
                existing.setOrderedQuantity(existing.getOrderedQuantity() + p.getOrderedQuantity());
            } else {
                // Make a shallow copy to avoid modifying the original
                grouped.put(id,new Product(p.getProductId(),p.getProductDescription(),
                        p.getProductImageName(),p.getUnitPrice(),p.getStockQuantity()));
            }
        }
        return new ArrayList<>(grouped.values());
    }

    void cancel(){
        trolley.clear();
        displayTaTrolley="";

        //closes any old “insufficient stock” window.
        if (removeProductNotifier != null) {
            removeProductNotifier.closeNotifierWindow();
        }
        updateView();
    }
    void closeReceipt(){
        displayTaReceipt="";
    }

    void updateView() {
        if(theProduct != null){
            imageName = theProduct.getProductImageName();
            String relativeImageUrl = StorageLocation.imageFolder +imageName; //relative file path, eg images/0001.jpg
            // Get the full absolute path to the image
            Path imageFullPath = Paths.get(relativeImageUrl).toAbsolutePath();
            imageName = imageFullPath.toUri().toString(); //get the image full Uri then convert to String
            System.out.println("Image absolute path: " + imageFullPath); // Debugging to ensure path is correct
        }
        else{
            imageName = "imageHolder.jpg";
        }
        cusView.update(imageName, displayLaSearchResult, displayTaTrolley,displayTaReceipt);
    }
     // extra notes:
     //Path.toUri(): Converts a Path object (a file or a directory path) to a URI object.
     //File.toURI(): Converts a File object (a file on the filesystem) to a URI object

    //for test only
    public ArrayList<Product> getTrolley() {
        return trolley;
    }
    public void setTheProduct(Product theProduct) {
        this.theProduct = theProduct;
    }
}
