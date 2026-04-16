CREATE TABLE IF NOT EXISTS journeys (
  service_name VARCHAR(255) PRIMARY KEY,
  json TEXT NOT NULL
);

INSERT INTO journeys (service_name, json) VALUES
('example-survey', '{
  "pages": [
    {
      "type": "contentPage",
      "title": "Welcome to our survey",
      "index": 1
    },
    {
      "type": "string",
      "title": "What is your name?",
      "index": 2,
      "validation": "^[a-zA-Z\\s]{1,100}$"
    },
    {
      "type": "datePage",
      "title": "What is your date of birth?",
      "index": 3
    },
    {
      "type": "boolean",
      "title": "Do you have a driver''s license?",
      "index": {
        "true": 4,
        "false": 6
      }
    },
    {
      "type": "radioButton",
      "title": "What type of vehicle do you own?",
      "index": {
        "Car": 5,
        "Motorcycle": 5,
        "Truck": 5,
        "Other": 6
      },
      "options": [
        "Car",
        "Motorcycle",
        "Truck",
        "Other"
      ]
    },
    {
      "type": "checkbox",
      "title": "Select all vehicle features you have:",
      "index": 7,
      "options": [
        "GPS Navigation",
        "Backup Camera",
        "Cruise Control",
        "Sunroof",
        "Heated Seats"
      ]
    },
    {
      "type": "radioButton",
      "title": "What is your preferred transportation method?",
      "index": {
        "Public Transport": 8,
        "Personal Vehicle": 8,
        "Bicycle": 8,
        "Walking": 8
      },
      "options": [
        "Public Transport",
        "Personal Vehicle",
        "Bicycle",
        "Walking"
      ]
    },
    {
      "type": "multipleQuestionsPage",
      "title": "Rate your experience",
      "index": 9,
      "questions": [
        {
          "questionTitle": "How satisfied are you with our service?"
        },
        {
          "questionTitle": "How likely are you to recommend us?"
        }
      ],
      "validation": [
        "^[1-5]$",
        "^[1-5]$"
      ]
    },
    {
      "type": "contentPage",
      "title": "Thank you for completing the survey!",
      "index": 10
    }
  ]
}')
ON CONFLICT (service_name) DO NOTHING;
