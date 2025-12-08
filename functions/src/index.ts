import * as functions from "firebase-functions";

export const helloWorld = functions.https.onRequest(
  (request: functions.https.Request, response: functions.Response) => {
    const name = request.query.name ?? "EduQuiz";
    response.json({
      message: `Hola ${name}!`,
      timestamp: new Date().toISOString(),
    });
  },
);
