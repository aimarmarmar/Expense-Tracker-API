package expense_tracker;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import org.springframework.security.test.context.support.WithMockUser;

import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)

class ExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "testUser")
    void shouldApplyMonthFilter() throws Exception {

        mockMvc.perform(get("/expenses/summary")
                        .param("filter", "month"))
                .andExpect(status().isOk())

                // existing
                .andExpect(jsonPath("$.period.filter").value("month"))

                // NEW
                .andExpect(jsonPath("$.totalExpenses").exists())
                .andExpect(jsonPath("$.categories").isArray())
                .andExpect(jsonPath("$.categories.length()").value(greaterThanOrEqualTo(0)));
    }

    @Test
    @WithMockUser(username = "testUser")
    void shouldReturnSummaryWithoutFilter() throws Exception {

        mockMvc.perform(get("/expenses/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period.filter").value("all"))
                .andExpect(jsonPath("$.totalExpenses").exists());
    }

    @Test
    @WithMockUser(username = "testUser")
    void shouldFailForInvalidFilter() throws Exception {

        mockMvc.perform(get("/expenses/summary")
                        .param("filter", "random"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testUser")
    void shouldHandleEmptyData() throws Exception {

        mockMvc.perform(get("/expenses/summary")
                        .param("filter", "month"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories").isEmpty());
    }


}
