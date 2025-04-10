app.post('/create-payment-intent', async (req, res) => {
    try {
        const { amount, currency, email, userId } = req.body;
        console.log('Creating payment intent:', { amount, currency, email, userId });

        if (!amount || amount <= 0) {
            throw new Error('Invalid amount');
        }

        if (!email) {
            throw new Error('User email not found');
        }

        if (!userId) {
            throw new Error('User ID is required');
        }

        // Get or create customer
        const customerId = await getOrCreateCustomer(userId, email);
        console.log('Customer ID:', customerId);

        // Create ephemeral key
        const ephemeralKey = await stripe.ephemeralKeys.create(
            { customer: customerId },
            { apiVersion: '2023-10-16' }
        );

        // Create payment intent
        const paymentIntent = await stripe.paymentIntents.create({
            amount: amount,
            currency: currency || 'cad',
            customer: customerId,
            payment_method_types: ['card'],
            metadata: {
                userId: userId
            }
        });

        console.log('Payment intent created:', paymentIntent.id);

        res.json({
            clientSecret: paymentIntent.client_secret,
            customer: customerId,
            ephemeralKey: ephemeralKey.secret
        });
    } catch (error) {
        console.error('Error creating payment intent:', error);
        res.status(500).json({ 
            error: error.message || 'An error occurred while creating the payment intent'
        });
    }
}); 