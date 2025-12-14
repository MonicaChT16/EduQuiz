const admin = require('firebase-admin');
const path = require('path');
const fs = require('fs');

// Ruta al archivo serviceAccountKey.json
const serviceAccountPath = path.join(__dirname, '..', 'serviceAccountKey.json');

if (!fs.existsSync(serviceAccountPath)) {
    console.error('❌ ERROR: No se encontró serviceAccountKey.json');
    process.exit(1);
}

const serviceAccount = require(serviceAccountPath);

// Inicializar Firebase Admin
admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function checkPackQuestions() {
    console.log('🔍 Analizando preguntas del pack pack_2025_w01...\n');

    try {
        // 1. Obtener el pack
        const packDoc = await db.collection('packs').doc('pack_2025_w01').get();
        if (!packDoc.exists) {
            console.error('❌ El pack pack_2025_w01 no existe');
            return;
        }

        const packData = packDoc.data();
        console.log('📦 Pack:');
        console.log(`   - packId: ${packData.packId}`);
        console.log(`   - weekLabel: ${packData.weekLabel}`);
        console.log(`   - status: ${packData.status}`);
        console.log(`   - textIds: ${packData.textIds?.length || 0} textos`);
        console.log(`   - questionIds: ${packData.questionIds?.length || 0} preguntas\n`);

        // 2. Contar preguntas por subject
        const questionIds = packData.questionIds || [];
        console.log(`📊 Analizando ${questionIds.length} preguntas...\n`);

        const questionsBySubject = {};
        const questionsByText = {};

        for (const questionId of questionIds) {
            const questionDoc = await db.collection('questions').doc(questionId).get();
            if (questionDoc.exists) {
                const questionData = questionDoc.data();
                const textId = questionData.textId;

                // Obtener el texto para saber el subject
                const textDoc = await db.collection('texts').doc(textId).get();
                if (textDoc.exists) {
                    const textData = textDoc.data();
                    const subject = textData.subject || 'SIN_SUBJECT';

                    // Contar por subject
                    if (!questionsBySubject[subject]) {
                        questionsBySubject[subject] = [];
                    }
                    questionsBySubject[subject].push({
                        questionId,
                        textId,
                        title: textData.title,
                        prompt: questionData.prompt
                    });

                    // Contar por texto
                    if (!questionsByText[textId]) {
                        questionsByText[textId] = {
                            title: textData.title,
                            subject: subject,
                            questions: []
                        };
                    }
                    questionsByText[textId].questions.push(questionId);
                } else {
                    console.warn(`⚠️  Texto ${textId} no encontrado para pregunta ${questionId}`);
                }
            } else {
                console.warn(`⚠️  Pregunta ${questionId} no encontrada`);
            }
        }

        // 3. Mostrar resumen por subject
        console.log('📚 PREGUNTAS POR SUBJECT:');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        Object.keys(questionsBySubject).sort().forEach(subject => {
            const count = questionsBySubject[subject].length;
            console.log(`\n${subject}: ${count} preguntas`);
            questionsBySubject[subject].forEach((q, index) => {
                if (index < 3) {
                    console.log(`   ${index + 1}. ${q.questionId} - "${q.title}"`);
                }
            });
            if (count > 3) {
                console.log(`   ... y ${count - 3} más`);
            }
        });

        // 4. Mostrar resumen por texto
        console.log('\n\n📄 PREGUNTAS POR TEXTO:');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        Object.keys(questionsByText).forEach(textId => {
            const info = questionsByText[textId];
            console.log(`\n${textId} (${info.subject}):`);
            console.log(`   Título: "${info.title}"`);
            console.log(`   Preguntas: ${info.questions.length}`);
            info.questions.forEach(qId => {
                console.log(`     - ${qId}`);
            });
        });

        // 5. Resumen total
        console.log('\n\n📊 RESUMEN TOTAL:');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log(`Total de preguntas en Firestore: ${questionIds.length}`);
        Object.keys(questionsBySubject).forEach(subject => {
            console.log(`  ${subject}: ${questionsBySubject[subject].length} preguntas`);
        });

        // 6. Verificar problemas comunes
        console.log('\n\n🔍 VERIFICACIONES:');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

        // Verificar subject "LECTURA" vs "COMPRENSION_LECTORA"
        const lecturaCount = questionsBySubject['LECTURA']?.length || 0;
        const comprensionCount = questionsBySubject['COMPRENSION_LECTORA']?.length || 0;
        if (lecturaCount > 0) {
            console.log(`⚠️  PROBLEMA POTENCIAL: Hay ${lecturaCount} preguntas con subject "LECTURA"`);
            console.log(`   La app busca "COMPRENSION_LECTORA". El código tiene mapeo, pero verifica que funcione.`);
        }
        if (comprensionCount > 0) {
            console.log(`✓ Hay ${comprensionCount} preguntas con subject "COMPRENSION_LECTORA"`);
        }

        // Verificar si hay menos de 10 preguntas por subject
        Object.keys(questionsBySubject).forEach(subject => {
            const count = questionsBySubject[subject].length;
            if (count < 10) {
                console.log(`⚠️  ADVERTENCIA: ${subject} tiene solo ${count} preguntas (se requieren 10 para un examen completo)`);
            } else {
                console.log(`✓ ${subject} tiene ${count} preguntas (suficiente)`);
            }
        });

        console.log('\n✅ Análisis completado\n');

    } catch (error) {
        console.error('❌ Error:', error);
        console.error(error.stack);
    }
}

checkPackQuestions()
    .then(() => process.exit(0))
    .catch(error => {
        console.error('❌ Error fatal:', error);
        process.exit(1);
    });