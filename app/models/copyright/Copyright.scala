package models.copyright

import org.joda.time.DateTime

/**
 * Created by Zeuko on 05.04.14.
 */
case class Copyright(
                          title: String,
                          correspondingAuthor: CorrespondingAuthor,
                          contribution: List[Contribution],
                          financialDisclosure: String
                          ) {

}
