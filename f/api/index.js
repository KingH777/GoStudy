import axios from 'axios'

const API_URL = 'http://localhost:8080/api'

const api = axios.create({
    baseURL: API_URL,
    headers: {
        'Content-Type': 'application/json'
    }
})

export default {
    // Finance records
    getAllRecords() {
        return api.get('/finance')
    },
    getRecord(id) {
        return api.get(`/finance/${id}`)
    },
    createRecord(record) {
        return api.post('/finance', record)
    },
    updateRecord(id, record) {
        return api.put(`/finance/${id}`, record)
    },
    deleteRecord(id) {
        return api.delete(`/finance/${id}`)
    },
    getStatistics() {
        return api.get('/finance/statistics')
    },
    clearAllData() {
        return api.delete('/finance/clear')
    },

    // User authentication
    login(credentials) {
        return api.post('/users/login', credentials)
    },
    changePassword(passwordData) {
        return api.post('/users/change-password', passwordData)
    }
}