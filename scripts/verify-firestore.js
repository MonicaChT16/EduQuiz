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

async function verifyFirestore() {
    console.log('🔍 Verificando Firestore...\n');

    try {
        // 1. Verificar que existe el pack
        console.log('1️⃣ Verificando pack pack_2025_w01...');
        const packRef = db.collection('packs').doc('pack_2025_w01');
        const packDoc = await packRef.get();
        
        if (!packDoc.exists) {
            console.error('   ❌ El pack pack_2025_w01 NO existe');
            console.log('   💡 Ejecuta: node scripts/init-firestore.js');
            return;
        }
        
        const packData = packDoc.data();
        console.log('   ✅ Pack existe');
        console.log(`   - packId: ${packData.packId}`);
        console.log(`   - weekLabel: ${packData.weekLabel}`);
        console.log(`   - status: ${packData.status}`);
        console.log(`   - publishedAt: ${packData.publishedAt}`);
        
        if (packData.status !== 'PUBLISHED') {
            console.error(`   ❌ ERROR: status debe ser "PUBLISHED" pero es "${packData.status}"`);
            console.log('   💡 Actualiza el status a "PUBLISHED" en Firebase Console');
        } else {
            console.log('   ✅ Status es PUBLISHED');
        }
        
        if (!packData.publishedAt || typeof packData.publishedAt !== 'number') {
            console.error(`   ❌ ERROR: publishedAt debe ser un número pero es: ${typeof packData.publishedAt}`);
        } else {
            console.log('   ✅ publishedAt es un número');
        }
        
        // 2. Verificar textos
        console.log('\n2️⃣ Verificando textos...');
        const textIds = packData.textIds || [];
        console.log(`   - textIds esperados: ${textIds.length}`);
        
        for (const textId of textIds) {
            const textRef = db.collection('texts').doc(textId);
            const textDoc = await textRef.get();
            if (textDoc.exists) {
                console.log(`   ✅ Texto ${textId} existe`);
            } else {
                console.error(`   ❌ Texto ${textId} NO existe`);
            }
        }
        
        // 3. Verificar preguntas
        console.log('\n3️⃣ Verificando preguntas...');
        const questionIds = packData.questionIds || [];
        console.log(`   - questionIds esperados: ${questionIds.length}`);
        
        for (const questionId of questionIds) {
            const questionRef = db.collection('questions').doc(questionId);
            const questionDoc = await questionRef.get();
            if (questionDoc.exists) {
                console.log(`   ✅ Pregunta ${questionId} existe`);
            } else {
                console.error(`   ❌ Pregunta ${questionId} NO existe`);
            }
        }
        
        // 4. Verificar consulta
        console.log('\n4️⃣ Verificando consulta de packs publicados...');
        const publishedPacks = await db.collection('packs')
            .where('status', '==', 'PUBLISHED')
            .get();
        
        console.log(`   - Packs publicados encontrados: ${publishedPacks.size}`);
        if (publishedPacks.size === 0) {
            console.error('   ❌ No se encontraron packs publicados');
        } else {
            publishedPacks.forEach(doc => {
                console.log(`   ✅ Pack encontrado: ${doc.id} (${doc.data().weekLabel})`);
            });
        }
        
        console.log('\n✅ Verificación completada');
        
    } catch (error) {
        console.error('❌ Error durante la verificación:', error.message);
        console.error(error);
    }
}

verifyFirestore().then(() => {
    process.exit(0);
}).catch(error => {
    console.error('❌ Error fatal:', error);
    process.exit(1);
});

