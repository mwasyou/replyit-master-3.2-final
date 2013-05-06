package com.sapienter.jbilling.server.pricing.strategy;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.PricingTestHelper;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.math.BigDecimal;

import static com.sapienter.jbilling.test.Asserts.*;
import static org.testng.AssertJUnit.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * @author Brian Cowdery
 * @since 10-Jul-2012
 */
@Test(groups = { "web-services", "pricing", "graduated" })
public class GraduatedPricingWSTest {

    JbillingAPI api;

    @BeforeTest
    public void getAPI() throws Exception {
        api = JbillingAPIFactory.getAPI();
    }

    @Test
    public void testGraduatedPricing() {
        // new item with graduated pricing
        // overage is charged at $1.00/unit
        PriceModelWS graduatedPrice = new PriceModelWS(PriceModelStrategy.GRADUATED.name(), new BigDecimal("1.00"), 1);
        graduatedPrice.addAttribute("included", "20");  // 20 units included

        ItemDTOEx item = PricingTestHelper.buildItem("TEST-GRADUATED", "Graduated pricing test");
        item.addDefaultPrice(CommonConstants.EPOCH_DATE, graduatedPrice);

        item.setId(api.createItem(item));
        assertNotNull("item created", item.getId());


        // create user to test pricing with
        UserWS user = PricingTestHelper.buildUser("graduated-pricing");
        user.setUserId(api.createUser(user));
        assertNotNull("customer created", user.getUserId());

        // order to be rated to test pricing
        OrderWS order = PricingTestHelper.buildMonthlyOrder(user.getUserId());
        OrderLineWS line = PricingTestHelper.buildOrderLine(item.getId(), 20);
        order.setOrderLines(new OrderLineWS[] { line });

        // quantity of 20 is within the graduated pricing, priced at $0.00
        order = api.rateOrder(order);

        assertThat(order.getOrderLines().length, is(1));
        assertEquals(BigDecimal.ZERO, order.getOrderLines()[0].getAmountAsDecimal());

        // quantity of 21 exceeds included quantity, priced at $1.00 per unit over 20
        line.setQuantity(21);
        order.setOrderLines(new OrderLineWS[] { line });
        order = api.rateOrder(order);

        assertThat(order.getOrderLines().length, is(1));
        assertEquals(new BigDecimal("1.00"), order.getOrderLines()[0].getAmountAsDecimal());


        // cleanup
        api.deleteItem(item.getId());
        api.deleteUser(user.getUserId());
    }
}
