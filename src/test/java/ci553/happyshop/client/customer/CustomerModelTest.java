package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Product;
import ci553.happyshop.storageAccess.DatabaseRW;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;


class CustomerModelTest {

    //Testing the adding Merges items with the same product ID (combining their quantities).

    @Test
    void makeOrganizedTrolley() {
        CustomerModel cm = new CustomerModel();
        Product p = new Product("0001","TV", "0001.jpg", 12.01, 100);
        cm.setTheProduct(p);
        cm.makeOrganizedTrolley();
        cm.makeOrganizedTrolley();
        cm.makeOrganizedTrolley();
        ArrayList<Product> tro = cm.getTrolley();
        assertEquals(1, tro.size());
        assertEquals(3, tro.getFirst().getOrderedQuantity());
    }

    @Test
    void checkOut_removesInsufficientProductsFromTrolley() throws IOException, SQLException {
        // Arrange
        CustomerModel cm = new CustomerModel();

        // Attach a dummy view so updateView() doesn't crash
        cm.cusView = new CustomerView() {
            @Override
            public void update(String imageName, String searchResult, String trolley, String receipt) {
                // do nothing in tests
            }
        };

        // Put two products in the trolley
        Product p1 = new Product("0001", "TV", "0001.jpg", 12.01, 100);
        p1.setOrderedQuantity(2);   // customer wants 2 TVs
        Product p2 = new Product("0002", "Phone", "0002.jpg", 20.00, 100);
        p2.setOrderedQuantity(5);   // customer wants 5 phones

        cm.getTrolley().add(p1);
        cm.getTrolley().add(p2);

        // Fake database: says that p2 (0002) has insufficient stock
        FakeDatabaseRW fakeDb = new FakeDatabaseRW(p2);
        cm.databaseRW = fakeDb;

        // No need for notifier in this test
        cm.removeProductNotifier = null;

        // Act
        cm.checkOut();

        // Assert: product 0002 should be removed from the trolley
        ArrayList<Product> trolleyAfter = cm.getTrolley();
        assertEquals(1, trolleyAfter.size());
        assertEquals("0001", trolleyAfter.get(0).getProductId());
        assertEquals(2, trolleyAfter.get(0).getOrderedQuantity());
    }


    /**
     * A simple fake implementation of DatabaseRW for testing.
     * It only cares about purchaseStocks(); other methods can be left unimplemented
     * or given dummy bodies
     */
    static class FakeDatabaseRW implements DatabaseRW {

        private final ArrayList<Product> insufficient;

        FakeDatabaseRW(Product insufficientProduct) {
            this.insufficient = new ArrayList<>();
            this.insufficient.add(insufficientProduct);
        }

        @Override
        public ArrayList<Product> searchProduct(String keyword) throws SQLException {
            return null;
        }

        @Override
        public Product searchByProductId(String productId) throws SQLException {
            return null;
        }

        @Override
        public ArrayList<Product> purchaseStocks(ArrayList<Product> proList) throws SQLException {
            // Always report that the given "insufficient" product cannot be fulfilled
            return insufficient;
        }

        @Override
        public void updateProduct(String id, String des, double price, String imageName, int stock) throws SQLException {

        }

        @Override
        public void deleteProduct(String id) throws SQLException {

        }

        @Override
        public void insertNewProduct(String id, String des, double price, String image, int stock) throws SQLException {

        }

        @Override
        public boolean isProIdAvailable(String productId) throws SQLException {
            return false;
        }
    }

    @Test
    void searchWithInput_usesNameSearchWhenIdNotFound() throws SQLException {
        // Arrange.
        CustomerModel cm = new CustomerModel();

        // Fake view to capture what the model sends to the UI.
        FakeCustomerView fakeView = new FakeCustomerView();
        cm.cusView = fakeView;

        // Fake database: ID search returns null, name search returns two products
        FakeSearchDatabaseRW fakeDb = new FakeSearchDatabaseRW();
        cm.databaseRW = fakeDb;

        // Act: ID "USB" doesn't exist, but name search will find two products
        cm.searchWithInput("USB");

        // Assert: view should have received the two products from name search
        assertNotNull(fakeView.lastSearchProducts);
        assertEquals(2, fakeView.lastSearchProducts.size());
        assertEquals("0007", fakeView.lastSearchProducts.get(0).getProductId());
        assertEquals("0008", fakeView.lastSearchProducts.get(1).getProductId());

        // And the label text should mention the Product_Id of the first result
        assertNotNull(fakeView.lastSearchLabel);
        assertTrue(fakeView.lastSearchLabel.contains("Product_Id: 0007"));
    }

    /** Simple fake view to record what the model sends */
    static class FakeCustomerView extends CustomerView {

        ArrayList<Product> lastSearchProducts;
        String lastSearchLabel;

        @Override
        public void update(String imageName, String searchResult, String trolley, String receipt) {
            // just remember the label text (search result area)
            lastSearchLabel = searchResult;
        }

        @Override
        void updateSearchResults(ArrayList<Product> products) {
            // remember the product list sent to the ListView
            lastSearchProducts = products;
        }
    }

    /** Fake database for unified search testing */
    static class FakeSearchDatabaseRW implements DatabaseRW {

        @Override
        public Product searchByProductId(String productId) throws SQLException {
            // Simulate "ID not found" → force name search path
            return null;
        }

        @Override
        public ArrayList<Product> searchProduct(String keyword) throws SQLException {
            // Simulate two products found by name "USB"
            ArrayList<Product> list = new ArrayList<>();
            Product p1 = new Product("0007", "USB drive", "0007.jpg", 6.99, 100);
            Product p2 = new Product("0008", "USB2 drive", "0008.jpg", 7.99, 100);
            list.add(p1);
            list.add(p2);
            return list;
        }

        // The rest of DatabaseRW methods can be dummy because they are not used in this test:

        @Override
        public ArrayList<Product> purchaseStocks(ArrayList<Product> proList) throws SQLException {
            return null;
        }

        @Override
        public void updateProduct(String id, String des, double price, String imageName, int stock) throws SQLException { }

        @Override
        public void deleteProduct(String id) throws SQLException { }

        @Override
        public void insertNewProduct(String id, String des, double price, String image, int stock) throws SQLException { }

        @Override
        public boolean isProIdAvailable(String productId) throws SQLException {
            return false;
        }
    }
}