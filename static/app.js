// <.>
const root = '/api'

async function customers() {
    const response = await fetch(root + '/customers')
    return await response.json()
}

async function me() {
    const response = await fetch(root + '/me')
    return await response.json()
}


// <.>
async function email(customerId) {
    const response = await fetch(root + '/email?customerId=' + customerId, {method: 'POST'})
    const data = await response.json()
    const cssQuerySelector = '#' + divIdFor({id: customerId}) + ' .id'
    document.querySelector(cssQuerySelector).classList.add('fade')
    return data
}

// <.>
function divIdFor(customer) {
    return 'customerDiv' + customer.id
}

// <.>
window.addEventListener('load', async (event) => {
    document.getElementById('me').innerHTML = (await me()).name;
    const customersDiv = document.getElementById('customers')
    const customersResults = await customers();
    customersResults.forEach(customer => {
        const div = document.createElement('div')
        div.innerHTML = `
              <button type="button">email report</button>
              <span class="id"> ${customer.id} </span>
              <span> ${customer.name}</span>
            `
        div.id = divIdFor(customer)
        customersDiv.appendChild(div)
        document
            .querySelector('#' + divIdFor(customer))
            .addEventListener('click', () => email(customer.id))
    });
})