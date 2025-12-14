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

// Mapeo de valores antiguos a nuevos
const subjectMapping = {
    'LECTURA': 'COMPRENSION_LECTORA',
    'LECTURA_COMPRENSION': 'COMPRENSION_LECTORA',
    'COMPRENSION': 'COMPRENSION_LECTORA',
    'MATEMATICA': 'MATEMATICA',
    'MATEMATICAS': 'MATEMATICA',
    'MATH': 'MATEMATICA',
    'CIENCIAS': 'CIENCIAS',
    'CIENCIA': 'CIENCIAS',
    'SCIENCE': 'CIENCIAS'
};

async function updateFirestoreSubjects() {
    console.log('🔄 Actualizando subjects en Firestore...\n');

    try {
        // 1. Actualizar todos los textos
        console.log('📚 Actualizando textos...');
        const textsSnapshot = await db.collection('texts').get();
        let textsUpdated = 0;
        let textsSkipped = 0;

        for (const doc of textsSnapshot.docs) {
            const data = doc.data();
            const currentSubject = data.subject;
            
            if (!currentSubject) {
                console.log(`   ⚠️  Texto ${doc.id} no tiene subject, saltando...`);
                textsSkipped++;
                continue;
            }

            const normalizedSubject = currentSubject.toUpperCase();
            const newSubject = subjectMapping[normalizedSubject] || normalizedSubject;

            if (newSubject !== currentSubject) {
                await doc.ref.update({ subject: newSubject });
                console.log(`   ✅ Texto ${doc.id}: "${currentSubject}" -> "${newSubject}"`);
                textsUpdated++;
            } else {
                console.log(`   ✓ Texto ${doc.id}: "${currentSubject}" (ya correcto)`);
                textsSkipped++;
            }
        }

        console.log(`\n📊 Resumen de textos:`);
        console.log(`   ✅ Actualizados: ${textsUpdated}`);
        console.log(`   ✓ Sin cambios: ${textsSkipped}`);
        console.log(`   📝 Total: ${textsSnapshot.size}`);

        // 2. Verificar que los packs tengan los datos correctos
        console.log('\n📦 Verificando packs...');
        const packsSnapshot = await db.collection('packs').get();
        
        for (const doc of packsSnapshot.docs) {
            const data = doc.data();
            console.log(`   ✓ Pack ${doc.id}: ${data.weekLabel || 'N/A'}`);
            
            if (data.textIds && Array.isArray(data.textIds)) {
                console.log(`      - Textos: ${data.textIds.length}`);
            }
            if (data.questionIds && Array.isArray(data.questionIds)) {
                console.log(`      - Preguntas: ${data.questionIds.length}`);
            }
        }

        console.log('\n✅ Actualización completada');
        console.log('\n💡 Valores de subject válidos:');
        console.log('   - COMPRENSION_LECTORA');
        console.log('   - MATEMATICA');
        console.log('   - CIENCIAS');

    } catch (error) {
        console.error('❌ Error durante la actualización:', error.message);
        console.error(error);
        process.exit(1);
    }
}

updateFirestoreSubjects()
    .then(() => {
        console.log('\n✨ Proceso completado exitosamente');
        process.exit(0);
    })
    .catch((error) => {
        console.error('\n❌ Error fatal:', error);
        process.exit(1);
    });










