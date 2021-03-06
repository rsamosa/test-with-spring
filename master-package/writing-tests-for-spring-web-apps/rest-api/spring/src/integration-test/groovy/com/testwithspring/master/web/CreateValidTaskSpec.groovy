package com.testwithspring.master.web

import com.github.springtestdbunit.DbUnitTestExecutionListener
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DbUnitConfiguration
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader
import com.testwithspring.master.IdColumnReset
import com.testwithspring.master.IntegrationTest
import com.testwithspring.master.IntegrationTestContext
import com.testwithspring.master.Tasks
import com.testwithspring.master.Users
import com.testwithspring.master.common.ConstantDateTimeService
import com.testwithspring.master.config.Profiles
import com.testwithspring.master.task.TaskFormDTO
import com.testwithspring.master.task.TaskStatus
import org.junit.experimental.categories.Category
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import spock.lang.Specification

import static org.hamcrest.Matchers.hasSize
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.nullValue
import static org.hamcrest.Matchers.nullValue
import static org.hamcrest.Matchers.nullValue
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@ContextConfiguration(classes = [IntegrationTestContext.class])
@WebAppConfiguration
@TestExecutionListeners(value = DbUnitTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
@DatabaseSetup([
        '/com/testwithspring/master/users.xml',
        '/com/testwithspring/master/no-tasks-and-tags.xml'
])
@DbUnitConfiguration(dataSetLoader = ReplacementDataSetLoader.class)
@Category(IntegrationTest.class)
@ActiveProfiles(Profiles.INTEGRATION_TEST)
class CreateValidTaskSpec extends Specification {

    @Autowired
    WebApplicationContext webAppContext

    @Autowired
    IdColumnReset idColumnReset

    def mockMvc

    def setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webAppContext)
                .apply(springSecurity())
                .build()

        idColumnReset.resetIdColumns('tasks')
    }

    @ExpectedDatabase(value = '/com/testwithspring/master/no-tasks-and-tags.xml', assertionMode = DatabaseAssertionMode.NON_STRICT)
    def 'Create task as an anonymous user'() {

        def response

        when: 'An anonymous user tries to create a task'
        response = createTask()

        then: 'Should return the HTTP status unauthorized'
        response.andExpect(status().isUnauthorized())

        and: 'Should return an empty response'
        response.andExpect(content().string(''))
    }

    @WithUserDetails(Users.JohnDoe.EMAIL_ADDRESS)
    @ExpectedDatabase(value = 'create-task-as-user-expected.xml', assertionMode = DatabaseAssertionMode.NON_STRICT)
    def 'Create task as a registered user'() {

        def response

        when: 'A registered user creates a task by using valid information'
        response = createTask()

        then: 'Should return the HTTP status code created'
        response.andExpect(status().isCreated())

        and: 'Should return the created task as json'
        response.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))

        and: 'Should return the information of the created task'
        response.andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.ASSIGNEE, nullValue()))
                .andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.CLOSER, nullValue()))
                .andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.CREATION_TIME, is(ConstantDateTimeService.CURRENT_DATE_AND_TIME)))
                .andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.Creator.ID, is(Users.JohnDoe.ID.intValue())))
                .andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.Creator.NAME, is(Users.JohnDoe.NAME)))
                .andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.ID, is(1)))
                .andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.MODIFICATION_TIME, is(ConstantDateTimeService.CURRENT_DATE_AND_TIME)))
                .andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.Modifier.ID, is(Users.JohnDoe.ID.intValue())))
                .andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.Modifier.NAME, is(Users.JohnDoe.NAME)))
                .andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.TITLE, is(Tasks.WriteExampleApp.TITLE)))
                .andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.DESCRIPTION, is(Tasks.WriteExampleApp.DESCRIPTION)))
                .andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.STATUS, is(TaskStatus.OPEN.toString())))
                .andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.RESOLUTION, nullValue()))

        and: 'Should return a task that has no tags'
        response.andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.TAGS, hasSize(0)))
    }

    @WithUserDetails(Users.AnneAdmin.EMAIL_ADDRESS)
    @ExpectedDatabase(value = 'create-task-as-admin-expected.xml', assertionMode = DatabaseAssertionMode.NON_STRICT)
    def 'Create task as an administrator'() {

        def response

        when: 'An administrator creates a task by using valid information'
        response = createTask()

        then: 'Should return the HTTP status code created'
        response.andExpect(status().isCreated())

        and: 'Should return the created task as json'
        response.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))

        and: 'Should return the information of the created task'
        response.andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.ASSIGNEE, nullValue()))
                .andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.CLOSER, nullValue()))
                .andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.CREATION_TIME, is(ConstantDateTimeService.CURRENT_DATE_AND_TIME)))
                .andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.Creator.ID, is(Users.AnneAdmin.ID.intValue())))
                .andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.Creator.NAME, is(Users.AnneAdmin.NAME)))
                .andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.ID, is(1)))
                .andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.MODIFICATION_TIME, is(ConstantDateTimeService.CURRENT_DATE_AND_TIME)))
                .andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.Modifier.ID, is(Users.AnneAdmin.ID.intValue())))
                .andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.Modifier.NAME, is(Users.AnneAdmin.NAME)))
                .andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.TITLE, is(Tasks.WriteExampleApp.TITLE)))
                .andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.DESCRIPTION, is(Tasks.WriteExampleApp.DESCRIPTION)))
                .andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.STATUS, is(TaskStatus.OPEN.toString())))
                .andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.RESOLUTION, nullValue()))

        and: 'Should return a task that has no tags'
        response.andExpect(jsonPath(WebTestConstants.JsonPathProperty.Task.TAGS, hasSize(0)))
    }

    private ResultActions createTask() {
        def input = new TaskFormDTO(title: Tasks.WriteExampleApp.TITLE, description: Tasks.WriteExampleApp.DESCRIPTION)
        return mockMvc.perform(post('/api/task')
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(ObjectIntoJsonConverter.convertObjectIntoJsonBytes(input))
                .with(csrf())
        )
    }
}
