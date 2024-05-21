Logical:         TrackedEntity
Id:              tracked-entity-logical-model
Title:           "Tracked Entity"
Description:     "An entity that is tracked throughout DHIS2."
* trackedEntity 1..1 string "Identifier of the tracked entity"
* attributes 0..* Attribute "List of tracked entity attribute values owned by the tracked entity"
* enrollments 0..* Enrollment "List of enrollments owned by the tracked entity"

Logical:        Attribute
Id:             attribute
Title:          "Attribute"
Description:    "A data point within a tracked entity or an enrollment."
* attribute 1..1 string "References the attribute definition ID that this attribute represents"
* value 1..1 string "Value of the tracked entity attribute"

Logical:        Enrollment
Id:             enrollment
Title:          "Enrollment"
Description:    "Enrollment"
* events 0..* Event "List of events owned by the enrollment"
* orgUnitName 1..1 string "Name of the organisation unit where the enrollment took place"
* attributes 0..* Attribute "List of tracked entity attribute values connected to the enrollment"

Logical:        Event
Id:             event
Title:          "Event"
Description:    "Event"
* programStage 1..1 string "Identifier of the program stage"
* createdAt 1..1 dateTime "Timestamp when the user created the event."
* dataValues 0..* DataValue "List of data values connected to the event"

Logical:        DataValue
Id:             data-value
Title:          "Data Value"
Description:    "A data point within an event."
* dataElement 1..1 string "Identifier of the data element this data value represents"
* value 1..1 string "Value of the data value"