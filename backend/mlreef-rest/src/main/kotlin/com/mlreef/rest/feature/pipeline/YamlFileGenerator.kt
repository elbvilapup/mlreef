package com.mlreef.rest.feature.pipeline

import com.mlreef.rest.Account
import com.mlreef.rest.DataProcessorInstance
import com.mlreef.rest.DataProcessorType
import org.springframework.core.io.ClassPathResource
import java.util.stream.Collectors


const val PIPELINE_TOKEN_SECRET = "EPF_BOT_SECRET"
const val GIT_PUSH_USER = "GIT_PUSH_USER"
const val GIT_PUSH_TOKEN = "GIT_PUSH_TOKEN"

const val EPF_IMAGE_TAG = "%EPF_IMAGE_TAG%"
const val EPF_GITLAB_HOST = "%EPF_GITLAB_HOST%"
const val EPF_PIPELINE_URL = "%EPF_PIPELINE_URL%"
const val EPF_PIPELINE_SECRET = "%EPF_PIPELINE_SECRET%"
const val CONF_EMAIL = "%CONF_EMAIL%"
const val CONF_NAME = "%CONF_NAME%"
const val SOURCE_BRANCH = "%SOURCE_BRANCH%"
const val TARGET_BRANCH = "%TARGET_BRANCH%"
const val PIPELINE_STRING = "%PIPELINE_STRING%"
const val NEWLINE = "\n"

internal object YamlFileGenerator {
    val template: String = ClassPathResource("mlreef-file-template.yml")
        .inputStream.bufferedReader().use {
            it.lines().collect(Collectors.joining(NEWLINE))
        }

    fun renderYaml(
        author: Account,
        epfPipelineSecret: String,
        epfPipelineUrl: String,
        epfGitlabUrl: String,
        epfImageTag: String,
        sourceBranch: String,
        targetBranch: String,
        dataProcessors: List<DataProcessorInstance>,
    ): String = template
        .replace(CONF_EMAIL, newValue = author.email)
        .replace(CONF_NAME, newValue = author.username)
        .replace(SOURCE_BRANCH, newValue = sourceBranch)
        .replace(TARGET_BRANCH, newValue = targetBranch)
        .replace(EPF_IMAGE_TAG, newValue = epfImageTag)
        .replace(EPF_PIPELINE_SECRET, newValue = epfPipelineSecret)
        .replace(EPF_GITLAB_HOST, epfGitlabUrl
            .removePrefix("http://")
            .removePrefix("https://")
            .substringBefore("/"))
        .replace(EPF_PIPELINE_URL,
            if (epfPipelineUrl.startsWith("http://")
                || epfPipelineUrl.startsWith("https://")) {
                epfPipelineUrl
            } else { "http://$epfPipelineUrl" }
        )
        .replace(PIPELINE_STRING,
            dataProcessors.joinToString(NEWLINE) { dpInstance ->
                val path = when (dpInstance.dataProcessor.type) {
                    DataProcessorType.ALGORITHM -> "/epf/model/"
                    DataProcessorType.OPERATION -> "/epf/pipelines/"
                    DataProcessorType.VISUALIZATION -> "/epf/visualisation/"
                }
                // the 4 space indentation is necessary for the yaml syntax
                "    python $path${dpInstance.processorVersion.command}.py " +
                    dpInstance.parameterInstances
                        .joinToString(" ") { "--${it.name} ${it.value}" }
            },
        )

}
