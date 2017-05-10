/*
 * Copyright 2017 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package realm.io.realmsurveys.controller

import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import io.realm.Realm
import org.jetbrains.anko.find
import realm.io.realmsurveys.R
import realm.io.realmsurveys.extensions.uniqueUserId
import realm.io.realmsurveys.model.Answer
import realm.io.realmsurveys.model.Question

class SurveyActivity : AppCompatActivity() {

    val recyclerView by lazy { find<RecyclerView>(R.id.questionsList) }
    val sharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(application) }
    lateinit var realm: Realm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_survey)
        realm = Realm.getDefaultInstance()

        val questions = realm
                .where(Question::class.java)
                .isEmpty("answers")
                .or()
                .beginGroup()
                .not()
                .contains("answers.userId", sharedPreferences.uniqueUserId())
                .endGroup()
                .findAllSortedAsync("timestamp")

        recyclerView.adapter = QuestionViewAdapter(questions, onQuestionAnswered)
    }

    override fun onDestroy() {
        super.onDestroy()
        realm.close()
    }

    val onQuestionAnswered = { questionId: String, response: Boolean ->

        val deviceUserId = sharedPreferences.uniqueUserId()

        realm.executeTransactionAsync { bgRealm ->

            val question = bgRealm.where(Question::class.java).equalTo("questionId", questionId).findFirst()

            var answer: Answer? = question?.answers?.where()
                    ?.equalTo("userId", deviceUserId)
                    ?.findFirst()

            if (answer == null) {
                answer = bgRealm.createObject(Answer::class.java)
                answer!!.userId = deviceUserId
                answer.question = question
                answer.response = response
                question.answers?.add(answer)
            }
        }


    }

}
