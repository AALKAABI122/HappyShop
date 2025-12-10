package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Product;

import java.io.IOException;
import java.sql.SQLException;

public class CustomerController {
    public CustomerModel cusModel;

    public void doAction(String action) throws SQLException, IOException {
        switch (action) {
            case "Search":
                cusModel.search();
                break;
            case "Add to Trolley":
                cusModel.addToTrolley();
                break;
            case "Cancel":
                cusModel.cancel();
                break;
            case "Check Out":
                cusModel.checkOut();
                break;
            case "OK & Close":
                cusModel.closeReceipt();
                break;
        }
    }
    /**
     * Called when the user selects a product from the search ListView in CustomerView.
     * This sets the current product in the model and refreshes the preview.
     */
    public void setSelectedProduct(Product product) {
        if (product != null && cusModel != null) {
            cusModel.setTheProduct(product);
            cusModel.updateView();  // refresh image + label for selected product.
        }
    }

}
