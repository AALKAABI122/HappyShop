package ci553.happyshop.client.customer;

import ci553.happyshop.utility.StorageLocation;
import ci553.happyshop.utility.UIStyle;
import ci553.happyshop.utility.WinPosManager;
import ci553.happyshop.utility.WindowBounds;
import ci553.happyshop.catalogue.Product;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * The CustomerView is separated into two sections by a line :
 *
 * 1. Search Page – Always visible, allowing customers to browse and search for products.
 * 2. the second page – display either the Trolley Page or the Receipt Page
 *    depending on the current context. Only one of these is shown at a time.
 */

public class CustomerView  {
    public CustomerController cusController;

    private final int WIDTH = UIStyle.customerWinWidth;
    private final int HEIGHT = UIStyle.customerWinHeight;
    private final int COLUMN_WIDTH = WIDTH / 2 - 10;

    private HBox hbRoot; // Top-level layout manager
    private VBox vbTrolleyPage;  //vbTrolleyPage and vbReceiptPage will swap with each other when need
    private VBox vbReceiptPage;

    TextField tfId; //for user input on the search page. Made accessible so it can be accessed or modified by CustomerModel
    TextField tfName; //for user input on the search page. Made accessible so it can be accessed by CustomerModel

    //four controllers needs updating when program going on
    private ImageView ivProduct; //image area in searchPage
    private Label lbProductInfo;//product text info in searchPage
    private TextArea taTrolley; //in trolley Page
    private TextArea taReceipt;//in receipt page

    // NEW: summary + observable list + listview (like Warehouse)
    private Label laSearchSummary;
    private ObservableList<Product> obProductList;
    private ListView<Product> lvProducts;

    // Holds a reference to this CustomerView window for future access and management
    // (e.g., positioning the removeProductNotifier when needed).
    private Stage viewWindow;

    public void start(Stage window) {
        VBox vbSearchPage = createSearchPage();
        vbTrolleyPage = CreateTrolleyPage();
        vbReceiptPage = createReceiptPage();

        // Create a divider line
        Line line = new Line(0, 0, 0, HEIGHT);
        line.setStrokeWidth(4);
        line.setStroke(Color.PINK);
        VBox lineContainer = new VBox(line);
        lineContainer.setPrefWidth(4); // Give it some space
        lineContainer.setAlignment(Pos.CENTER);

        hbRoot = new HBox(10, vbSearchPage, lineContainer, vbTrolleyPage); //initialize to show trolleyPage
        hbRoot.setAlignment(Pos.CENTER);
        hbRoot.setStyle(UIStyle.rootStyle);

        Scene scene = new Scene(hbRoot, WIDTH, HEIGHT);
        window.setScene(scene);
        window.setTitle("🛒 HappyShop Customer Client");
        WinPosManager.registerWindow(window,WIDTH,HEIGHT); //calculate position x and y for this window
        window.show();
        viewWindow=window;// Sets viewWindow to this window for future reference and management.
    }

    private VBox createSearchPage() {
        Label laPageTitle = new Label("Search by Product ID/Name");
        laPageTitle.setStyle(UIStyle.labelTitleStyle);

        // Unified ID/Name field
        Label laId = new Label("ID / Name:");
        laId.setStyle(UIStyle.labelStyle);
        laId.setMinWidth(80);

        tfId = new TextField();
        tfId.setPromptText("e.g. 0001 or USB");
        tfId.setStyle(UIStyle.textFiledStyle);
        tfId.setPrefWidth(180);
        // Pressing Enter = Search
        tfId.setOnAction(this::buttonClicked);

        Button btnSearch = new Button("Search");
        btnSearch.setStyle(UIStyle.buttonStyle);
        btnSearch.setMinWidth(70);
        btnSearch.setOnAction(this::buttonClicked);

        Button btnAddToTrolley = new Button("Add to Trolley");
        btnAddToTrolley.setStyle(UIStyle.buttonStyle);
        btnAddToTrolley.setMinWidth(110);
        btnAddToTrolley.setOnAction(this::buttonClicked);

        // Line 1: label + text field
        HBox hbIdLine = new HBox(10, laId, tfId);
        hbIdLine.setAlignment(Pos.CENTER_LEFT);

        // Line 2: buttons under the field
        HBox hbButtons = new HBox(10, btnSearch, btnAddToTrolley);
        hbButtons.setAlignment(Pos.CENTER_LEFT);


        // Summary label: "6 products found"
        laSearchSummary = new Label("No search yet");
        laSearchSummary.setStyle(UIStyle.labelStyle);

        // Observable list + ListView of products (similar to WarehouseView)
        obProductList = FXCollections.observableArrayList();
        lvProducts = new ListView<>(obProductList);
        lvProducts.setPrefHeight(200);
        lvProducts.setFixedCellSize(50);
        lvProducts.setStyle(UIStyle.listViewStyle);

        // Custom cell: image + text (reusing Warehouse idea)
        lvProducts.setCellFactory(param -> new ListCell<Product>() {
            @Override
            protected void updateItem(Product product, boolean empty) {
                super.updateItem(product, empty);

                if (empty || product == null) {
                    setGraphic(null);
                } else {
                    String imageName = product.getProductImageName(); // e.g. "0001.jpg"
                    String relativeImageUrl = StorageLocation.imageFolder + imageName;
                    Path imageFullPath = Paths.get(relativeImageUrl).toAbsolutePath();
                    String imageFullUri = imageFullPath.toUri().toString();

                    ImageView ivPro;
                    try {
                        ivPro = new ImageView(new Image(imageFullUri, 50, 45, true, true));
                    } catch (Exception e) {
                        ivPro = new ImageView(new Image("imageHolder.jpg", 50, 45, true, true));
                    }

                    Label laProToString = new Label(product.toString());
                    HBox hbox = new HBox(10, ivPro, laProToString);
                    setGraphic(hbox);
                }
            }
        });

        // When user selects a product in the list, tell controller/model
        lvProducts.getSelectionModel().selectedItemProperty().addListener((obs, oldP, newP) -> {
            if (newP != null && cusController != null) {
                cusController.setSelectedProduct(newP);  // we'll add this method
            }
        });

        // Small preview area (reuse your existing ivProduct + lbProductInfo)
        ivProduct = new ImageView("imageHolder.jpg");
        ivProduct.setFitHeight(60);
        ivProduct.setFitWidth(60);
        ivProduct.setPreserveRatio(true);
        ivProduct.setSmooth(true);

        lbProductInfo = new Label("Thank you for shopping with us.");
        lbProductInfo.setWrapText(true);
        lbProductInfo.setMinHeight(Label.USE_PREF_SIZE);
        lbProductInfo.setStyle(UIStyle.labelMulLineStyle);
        HBox hbSearchResult = new HBox(5, ivProduct, lbProductInfo);
        hbSearchResult.setAlignment(Pos.CENTER_LEFT);

        VBox vbSearchPage = new VBox(15,
                laPageTitle,
                hbIdLine,
                hbButtons,
                laSearchSummary,
                lvProducts,
                hbSearchResult
        );
        vbSearchPage.setPrefWidth(COLUMN_WIDTH);
        vbSearchPage.setAlignment(Pos.TOP_CENTER);
        vbSearchPage.setStyle("-fx-padding: 15px;");

        return vbSearchPage;
    }


    void updateSearchResults(ArrayList<Product> products) {
        if (products == null || products.isEmpty()) {
            laSearchSummary.setText("0 products found");
            obProductList.clear();
        } else {
            laSearchSummary.setText(products.size() + " products found");
            obProductList.setAll(products);
            // Auto-select the first product – this will also update the model via setSelectedProduct
            lvProducts.getSelectionModel().selectFirst();
        }
    }


    private VBox CreateTrolleyPage() {
        Label laPageTitle = new Label("🛒🛒  Trolley 🛒🛒");
        laPageTitle.setStyle(UIStyle.labelTitleStyle);

        taTrolley = new TextArea();
        taTrolley.setEditable(false);
        taTrolley.setPrefSize(WIDTH/2, HEIGHT-50);

        Button btnCancel = new Button("Cancel");
        btnCancel.setOnAction(this::buttonClicked);
        btnCancel.setStyle(UIStyle.buttonStyle);

        Button btnCheckout = new Button("Check Out");
        btnCheckout.setOnAction(this::buttonClicked);
        btnCheckout.setStyle(UIStyle.buttonStyle);

        HBox hbBtns = new HBox(10, btnCancel,btnCheckout);
        hbBtns.setStyle("-fx-padding: 15px;");
        hbBtns.setAlignment(Pos.CENTER);

        vbTrolleyPage = new VBox(15, laPageTitle, taTrolley, hbBtns);
        vbTrolleyPage.setPrefWidth(COLUMN_WIDTH);
        vbTrolleyPage.setAlignment(Pos.TOP_CENTER);
        vbTrolleyPage.setStyle("-fx-padding: 15px;");
        return vbTrolleyPage;
    }

    private VBox createReceiptPage() {
        Label laPageTitle = new Label("Receipt");
        laPageTitle.setStyle(UIStyle.labelTitleStyle);

        taReceipt = new TextArea();
        taReceipt.setEditable(false);
        taReceipt.setPrefSize(WIDTH/2, HEIGHT-50);

        Button btnCloseReceipt = new Button("OK & Close"); //btn for closing receipt and showing trolley page
        btnCloseReceipt.setStyle(UIStyle.buttonStyle);

        btnCloseReceipt.setOnAction(this::buttonClicked);

        vbReceiptPage = new VBox(15, laPageTitle, taReceipt, btnCloseReceipt);
        vbReceiptPage.setPrefWidth(COLUMN_WIDTH);
        vbReceiptPage.setAlignment(Pos.TOP_CENTER);
        vbReceiptPage.setStyle(UIStyle.rootStyleYellow);
        return vbReceiptPage;
    }


    private void buttonClicked(ActionEvent event) {
        try{

            Object src = event.getSource();
            String action;

            if (src instanceof Button btn) {
                action = btn.getText();
                if(action.equals("Add to Trolley")){
                    showTrolleyOrReceiptPage(vbTrolleyPage); //ensure trolleyPage shows if the last customer did not close their receiptPage
                }
                if(action.equals("OK & Close")){
                    showTrolleyOrReceiptPage(vbTrolleyPage);
                }
            } else if (src instanceof TextField) {
                // Enter in search field
                action = "Search";
            } else {
                return;
            }

            cusController.doAction(action);
        }
        catch(SQLException e){
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void update(String imageName, String searchResult, String trolley, String receipt) {

        ivProduct.setImage(new Image(imageName));
        lbProductInfo.setText(searchResult);
        taTrolley.setText(trolley);
        if (!receipt.equals("")) {
            showTrolleyOrReceiptPage(vbReceiptPage);
            taReceipt.setText(receipt);
        }
    }

    // Replaces the last child of hbRoot with the specified page.
    // the last child is either vbTrolleyPage or vbReceiptPage.
    private void showTrolleyOrReceiptPage(Node pageToShow) {
        int lastIndex = hbRoot.getChildren().size() - 1;
        if (lastIndex >= 0) {
            hbRoot.getChildren().set(lastIndex, pageToShow);
        }
    }

    WindowBounds getWindowBounds() {
        return new WindowBounds(viewWindow.getX(), viewWindow.getY(),
                  viewWindow.getWidth(), viewWindow.getHeight());
    }
}
